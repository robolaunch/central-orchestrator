package org.robolaunch.repository.concretes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.spi.ApplicationException;
import org.robolaunch.core.abstracts.GroupAdapter;
import org.robolaunch.core.abstracts.RandomGenerator;
import org.robolaunch.core.concretes.RandomGeneratorImpl;
import org.robolaunch.models.Artifact;
import org.robolaunch.models.Organization;
import org.robolaunch.models.kubernetes.V1MachineDeployment;
import org.robolaunch.models.kubernetes.V1VirtualCluster;
import org.robolaunch.repository.abstracts.AmazonRepository;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;
import org.robolaunch.repository.abstracts.GroupAdminRepository;
import org.robolaunch.repository.abstracts.KubernetesRepository;
import org.robolaunch.repository.abstracts.StorageRepository;
import org.robolaunch.service.ApiClientManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.gson.JsonElement;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1DeploymentStatus;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeAddress;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1NodeSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.ModelMapper;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import io.kubernetes.client.util.credentials.ClientCertificateAuthentication;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesListObject;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.kubernetes.client.util.generic.options.ListOptions;
import io.minio.errors.MinioException;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

@ApplicationScoped
public class CloudInstanceHelperRepositoryImpl implements CloudInstanceHelperRepository {
  private DynamicGraphQLClient graphqlClient;
  public static Integer VCCreated = 0;

  @ConfigProperty(name = "dns.zone")
  String dnsZoneName;
  @ConfigProperty(name = "kogito.dataindex.http.url")
  String kogitoDataIndexUrl;
  @ConfigProperty(name = "master.node.name")
  String masterNodeName;

  @Inject
  KubernetesRepository kubernetesRepository;
  @Inject
  GroupAdminRepository groupAdminRepository;
  @Inject
  GroupAdapter groupAdapter;
  @Inject
  AmazonRepository amazonRepository;
  @Inject
  StorageRepository storageRepository;
  @Inject
  JsonWebToken jwt;
  @Inject
  ApiClientManager apiClientManager;

  @PostConstruct
  public void initializeApis() throws IOException, ApiException, InterruptedException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, MinioException {
    graphqlClient = DynamicGraphQLClientBuilder.newBuilder()
        .url(kogitoDataIndexUrl + "/graphql/")
        .build();
  }

  @Override
  public String getCloudInstanceIP(String bufferName, String provider, String region, String superCluster)
      throws ApiException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, IOException, InterruptedException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    String namespaceName = getNamespaceNameWithBufferName(bufferName, provider, region, superCluster);
    String nodePort = "";
    for (V1Service service : coreV1Api
        .listNamespacedService(namespaceName, null, null, null, null, null, null, null,
            null, null, null)
        .getItems()) {
      Optional<String> svcName = Optional.ofNullable(service).map(V1Service::getMetadata)
          .map(m -> m.getName());

      if (svcName.get().equals("apiserver-svc")) {
        Optional<String> svcPort = Optional.ofNullable(service).map(V1Service::getSpec).map(m -> m.getPorts())
            .map(m -> m.get(0)).map(m -> m.getNodePort()).map(m -> m.toString());
        nodePort = svcPort.get();
        break;
      }
    }
    String masterIP = "";
    V1NodeList nodes = coreV1Api.listNode(null, null, null, null, "node-role.kubernetes.io/master", null,
        null, null, null, null);
    Optional<List<V1NodeAddress>> addresses = Optional.ofNullable(nodes).map(V1NodeList::getItems)
        .map(m -> m.get(0)).map(m -> m.getStatus()).map(m -> m.getAddresses());

    for (var address : addresses.get()) {
      if (address.getType().equals("ExternalIP")) {
        masterIP = address.getAddress();
        break;
      }
    }
    String serverIP = "https://" + masterIP + ":" + nodePort;
    return serverIP;
  }

  @Override
  public String generateBufferName() {
    RandomGenerator randomGenerator = new RandomGeneratorImpl();
    String randomString = randomGenerator.generateRandomString(8);
    return "vc-" + randomString.toLowerCase();
  }

  @Override
  public String getGeneratedMachineName(String bufferName, String provider, String region, String superCluster)
      throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException, ApiException,
      InterruptedException, MinioException {
    DynamicKubernetesApi machinesApi = apiClientManager.getMachineApi(provider, region, superCluster);
    String machineName = "";
    var list = machinesApi.list("kube-system").getObject().getItems();
    Iterator<DynamicKubernetesObject> iterator = list.iterator();
    while (iterator.hasNext()) {
      DynamicKubernetesObject obj = iterator.next();
      Optional<String> name = Optional.ofNullable(obj).map(DynamicKubernetesObject::getMetadata)
          .map(m -> m.getName());
      if (name.get()
          .contains(bufferName)) {
        machineName = obj.getMetadata().getName();
        break;
      }
    }
    return machineName;
  }

  @Override
  public Boolean nodeRefChecker(String bufferName, String machineName, String provider, String region,
      String superCluster)
      throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
      InterruptedException, MinioException {
    DynamicKubernetesApi machinesApi = apiClientManager.getMachineApi(provider, region, superCluster);
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    String nodeName = "";
    KubernetesApiResponse<DynamicKubernetesObject> machine = machinesApi.get("kube-system", machineName);
    if (machine.getObject().getRaw().get("status").getAsJsonObject().get("nodeRef") != null) {
      nodeName = machine.getObject().getRaw()
          .getAsJsonObject("status")
          .getAsJsonObject("nodeRef").get("name")
          .getAsString();

      V1Node node = coreV1Api.readNode(nodeName, null);
      Optional<List<V1NodeCondition>> conditions = Optional.ofNullable(node).map(V1Node::getStatus)
          .map(m -> m.getConditions());
      if (conditions.get().get(
          conditions.get().size()
              - 1)
          .getType().equals("Ready")
          && conditions.get().get(
              conditions.get()
                  .size()
                  - 1)
              .getStatus().equals("True")) {
        return true;
      } else {
        return false;
      }

    } else {
      return false;
    }

  }

  @Override
  public Boolean isVirtualClusterReady(String bufferName, String provider, String region, String superCluster)
      throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException, ApiException,
      InterruptedException, MinioException {
    DynamicKubernetesApi virtualClustersApi = apiClientManager.getVirtualClusterApi(provider, region, superCluster);
    ListOptions listOptions = new ListOptions();
    listOptions.setLabelSelector("robolaunch.io/buffer-instance=" + bufferName);
    var vcs = virtualClustersApi.list(listOptions);
    if (vcs.getObject().getItems().size() == 0) {
      return false;
    }
    for (var vc : vcs.getObject().getItems()) {
      Optional<String> vcName = Optional.ofNullable(vc).map(DynamicKubernetesObject::getMetadata)
          .map(m -> m.getName());
      if (vcName.get().equals(bufferName)) {
        if (vc.getRaw().get("status").getAsJsonObject().get("phase").getAsString()
            .equals("Running")) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String selectBufferedVirtualCluster(Integer vcCount, String provider, String region, String superCluster)
      throws KubectlException, IOException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, ApiException, MinioException {
    DynamicKubernetesApi virtualClustersApi = apiClientManager.getVirtualClusterApi(provider, region, superCluster);

    ModelMapper.addModelMap("tenancy.x-k8s.io", "v1alpha1", "VirtualCluster",
        "virtualclusters", true,
        V1VirtualCluster.class);

    ListOptions listOptions = new ListOptions();
    listOptions.setLabelSelector("buffered=true, !robolaunch.io/organization");
    var vcs = virtualClustersApi.list(listOptions);
    int randomInteger = (int) Math.floor(Math.random() * (vcCount));
    Optional<Map<String, String>> labels = Optional.ofNullable(vcs.getObject().getItems().get(randomInteger))
        .map(DynamicKubernetesObject::getMetadata)
        .map(m -> m.getLabels());
    String bufferName = labels.get()
        .get("robolaunch.io/buffer-instance");
    System.out.println("picked bn: " + bufferName);
    return bufferName;

  }

  @Override
  public String getNodeName(String machineName, String provider, String region, String superCluster)
      throws InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, IOException, ApiException, InterruptedException, MinioException {
    DynamicKubernetesApi machinesApi = apiClientManager.getMachineApi(provider, region, superCluster);
    KubernetesApiResponse<DynamicKubernetesObject> machine = machinesApi.get(
        "kube-system",
        machineName);
    for (JsonElement address : machine.getObject().getRaw().getAsJsonObject("status")
        .getAsJsonArray("addresses")) {
      if (address.getAsJsonObject().get("type").getAsString().equals("InternalDNS")) {
        return address.getAsJsonObject().get("address").getAsString();
      }
    }
    return null;

  }

  @Override
  public Boolean isStatefulSetsUp(String namespaceName, String provider, String region, String superCluster)
      throws ApiException, IOException,
      InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, InterruptedException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    var pods = coreV1Api.listNamespacedPod(namespaceName, null, null, null, null, null, null, null, null,
        null, null);
    Boolean etcdReady = false;
    Boolean apiServerReady = false;
    Boolean controllerManagerReady = false;

    for (V1Pod pod : pods.getItems()) {
      Optional<String> podName = Optional.ofNullable(pod).map(V1Pod::getMetadata)
          .map(m -> m.getName());
      Optional<String> phase = Optional.ofNullable(pod).map(V1Pod::getStatus)
          .map(m -> m.getPhase());
      Optional<List<V1ContainerStatus>> containerStatus = Optional.ofNullable(pod).map(V1Pod::getStatus)
          .map(m -> m.getContainerStatuses());
      if (podName.get().equals("etcd-0")) {
        if (phase.get().equals("Running")) {
          if (containerStatus.get().get(0).getReady()) {
            etcdReady = true;
          }
        }
      }
      if (podName.get().equals("apiserver-0")) {
        if (phase.get().equals("Running")) {
          if (containerStatus.get().get(0).getReady()) {
            apiServerReady = true;
          }
        }

      }
      if (podName.get().equals("controller-manager-0")) {
        if (phase.get().equals("Running")) {
          if (containerStatus.get().get(0).getReady()) {
            controllerManagerReady = true;
          }
        }

      }
    }
    return etcdReady && apiServerReady && controllerManagerReady;
  }

  @Override
  public Boolean isStatefulSetsDown(String namespaceName, String provider, String region, String superCluster)
      throws ApiException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, IOException, InterruptedException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    V1PodList pods = coreV1Api.listNamespacedPod(namespaceName, null, null, null, null, null, null, null, null,
        null, null);
    for (V1Pod pod : pods.getItems()) {
      Optional<String> podName = Optional.ofNullable(pod).map(V1Pod::getMetadata)
          .map(m -> m.getName());
      if (podName.get().equals("etcd-0") || podName.get()
          .equals("controller-manager-0")
          || podName.get()
              .equals("apiserver-0")) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean isCoreDNSDeploymentUp(String bufferName, String provider, String region, String superCluster)
      throws IOException, ApiException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, MinioException {
    ApiClient vcClient = getVirtualClusterClientWithBufferName(bufferName, provider, region, superCluster);

    AppsV1Api appsV1Api = new AppsV1Api(vcClient);
    V1DeploymentList deployments = appsV1Api.listNamespacedDeployment("kube-system", null, null, null, null,
        null, null, null, null, null, null);
    for (V1Deployment deployment : deployments.getItems()) {
      Optional<String> deploymentName = Optional.ofNullable(deployment).map(V1Deployment::getMetadata)
          .map(m -> m.getName());
      Optional<V1DeploymentStatus> deploymentStatus = Optional.ofNullable(deployment).map(V1Deployment::getStatus);
      Optional<Integer> replicas = Optional.ofNullable(deployment).map(V1Deployment::getStatus)
          .map(m -> m.getReadyReplicas());
      if (deploymentName.get().equals("coredns") && deploymentStatus.get().getReadyReplicas() != null
          && replicas.get() >= 1) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean isCertManagerReady(String bufferName, String provider, String region, String superCluster)
      throws ApiException, IOException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, MinioException {
    Boolean podsReady = false;
    Boolean svcReady = false;

    ApiClient apiClient = getVirtualClusterClientWithBufferName(bufferName, provider, region, superCluster);
    CoreV1Api vcCoreApi = new CoreV1Api(apiClient);

    V1PodList podList = vcCoreApi.listNamespacedPod("cert-manager", null, null, null, null, null, null, null, null,
        null, null);

    V1ServiceList serviceList = vcCoreApi.listNamespacedService("cert-manager", null, null, null, null,
        null, null, null, null, null, null);

    for (V1Pod pod : podList.getItems()) {
      Optional<String> podName = Optional.ofNullable(pod).map(V1Pod::getMetadata)
          .map(m -> m.getName());
      podsReady = true;
      if (podName.get().startsWith("cert-manager")) {
        Optional<String> phase = Optional.ofNullable(pod).map(V1Pod::getStatus)
            .map(m -> m.getPhase());
        System.out.println("phase: " + phase.get());
        if (!phase.get().equals("Running")) {
          podsReady = false;
        }
      }
    }

    for (V1Service svc : serviceList.getItems()) {
      Optional<String> svcName = Optional.ofNullable(svc).map(V1Service::getMetadata)
          .map(m -> m.getName());
      if (svcName.isPresent()) {
        if (svcName.get().equals("cert-manager")) {
          svcReady = true;
        }
      }

    }
    return podsReady && svcReady;
  }

  @Override
  public String selectNode(String bufferName, String provider, String region, String superCluster)
      throws ApiException, KubectlException, IOException, InterruptedException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    ApiClient apiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);
    while (true) {
      V1NodeList nodeList = coreV1Api.listNode(null, null, null, null,
          "!node-role.kubernetes.io/master, !robolaunch.io/buffer-instance, node-role.kubernetes.io/worker=worker",
          null, null, null, null, null);
      for (V1Node node : nodeList.getItems()) {
        Optional<V1ObjectMeta> nodeMetadata = Optional.ofNullable(node).map(V1Node::getMetadata);
        if (!isNodeReady(nodeMetadata.get().getName(), provider, region, superCluster)
            && isNodeUnschedulable(nodeMetadata.get().getName(), provider, region, superCluster)) {
          if (amazonRepository.getInstanceState(nodeMetadata.get().getName(), provider, region, superCluster)
              .equals("stopped")) {

            Kubectl.label(V1Node.class).name(nodeMetadata.get().getName())
                .apiClient(apiClient)
                .addLabel("robolaunch.io/buffer-instance", bufferName)
                .execute();
            return nodeMetadata.get().getName();
          }
        }

      }
      Thread.sleep(4000);
    }

  }

  @Override
  public Boolean isNodeUnschedulable(String nodeName, String provider, String region, String superCluster)
      throws ApiException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, IOException, InterruptedException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    var node = coreV1Api.readNode(nodeName, null);
    Optional<V1NodeSpec> nodeSpec = Optional.ofNullable(node).map(V1Node::getSpec);
    if (nodeSpec.get().getUnschedulable() == null) {
      return false;
    }
    var unschedulable = nodeSpec.get().getUnschedulable();

    return unschedulable;
  }

  @Override
  public Boolean isNodeReady(String nodeName, String provider, String region, String superCluster)
      throws ApiException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, IOException, InterruptedException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    var node = coreV1Api.readNode(nodeName, null);
    Optional<List<V1NodeCondition>> nodeConditions = Optional.ofNullable(node).map(V1Node::getStatus)
        .map(m -> m.getConditions());
    var lastCondition = nodeConditions.get()
        .get(nodeConditions.get().size() - 1);
    return lastCondition.getType().equals("Ready") && lastCondition
        .getStatus().equals("True");
  }

  @Override
  public Boolean isSubnetUsed(String bufferName, String provider, String region, String superCluster)
      throws ApiException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, IOException, InterruptedException, MinioException {
    DynamicKubernetesApi subnetsApi = apiClientManager.getSubnetApi(provider, region, superCluster);
    String namespaceName = getNamespaceNameWithBufferName(bufferName, provider, region, superCluster);

    var subnet = subnetsApi.get("subnet-" + namespaceName).getObject().getRaw();
    return subnet.getAsJsonObject().get("status").getAsJsonObject().get("v4usingIPs").getAsInt() > 0;
  }

  @Override
  public String findNode(String bufferName, Organization organization, String teamId,
      String cloudInstanceName, String provider, String region, String superCluster)
      throws ApiException, InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, IOException, InterruptedException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    V1NodeList nodeList = coreV1Api.listNode(null, null, null, null,
        "!node-role.kubernetes.io/master, robolaunch.io/organization=" + organization.getName()
            + ", robolaunch.io/team=" + teamId
            + ", robolaunch.io/cloud-instance=" + cloudInstanceName,
        null, null, null, null, null);
    Optional<V1ObjectMeta> nodeMetadata = Optional.ofNullable(nodeList.getItems().get(0)).map(V1Node::getMetadata);
    return nodeMetadata.get().getName();
  }

  @Override
  public Boolean isMachineCreated(String bufferName, String provider, String region, String superCluster)
      throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException, ApiException,
      InterruptedException, MinioException {
    DynamicKubernetesApi machinesApi = apiClientManager.getMachineApi(provider, region, superCluster);
    var machines = machinesApi.list();
    for (var machine : machines.getObject().getItems()) {
      Optional<String> machineName = Optional.ofNullable(machine).map(DynamicKubernetesObject::getMetadata)
          .map(m -> m.getName());
      if (machineName.get().contains(bufferName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean healthCheck(Organization organization, String teamId, String cloudInstanceName,
      String nodeName, String provider, String region, String superCluster) {
    Boolean isInstanceHealthy = amazonRepository.isRunning(nodeName, provider, region, superCluster);
    return isInstanceHealthy;
  }

  @Override
  public void deleteDNSRecord(Organization organization, String nodeName, String provider, String region,
      String superCluster)
      throws ApiException, InternalError, ApplicationException, IOException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, InterruptedException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    String externalIP = "";
    var node = coreV1Api.readNode(nodeName, null);
    Optional<List<V1NodeAddress>> nodeAddresses = Optional.ofNullable(node).map(V1Node::getStatus)
        .map(m -> m.getAddresses());
    for (int i = 0; i < nodeAddresses.get().size(); i++) {
      if (nodeAddresses.get().get(i).getType().equals(
          "ExternalIP")) {
        externalIP = nodeAddresses.get().get(i).getAddress().toString();
      }
    }
    String requestData = groupAdapter.toDeleteDNSRecord(organization, externalIP, dnsZoneName);
    String deleteDNSRecord = String.format("{\"id\": 0, \"method\": \"dnsrecord_del/1\", \"params\": %s} ",
        requestData);
    groupAdminRepository.makeRequest(deleteDNSRecord);
  }

  @Override
  public void deleteClusterVersion(String bufferName, String provider, String region, String superCluster)
      throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException, ApiException,
      InterruptedException, MinioException {
    DynamicKubernetesApi clusterVersionApi = apiClientManager.getClusterVersionApi(provider, region, superCluster);
    clusterVersionApi.delete("cv-" + bufferName);
  }

  @Override
  public void deleteOAuth2ProxyResources(String bufferName, String provider, String region, String superCluster)
      throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
      InterruptedException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    AppsV1Api appsApi = apiClientManager.getAppsApi(provider, region, superCluster);
    String namespaceName = getNamespaceNameWithBufferName(bufferName, provider, region, superCluster);

    Artifact artifact = new Artifact();
    artifact.setName("oauth2Proxy.yaml");
    String bucket = "template-artifacts";
    String yaml = storageRepository.getContent(artifact, bucket);
    List<Object> list = Yaml.loadAll(yaml);
    String newNamespace = namespaceName + "-oauth2-proxy";

    for (int i = 0; i < list.size(); i++) {
      Object obj = list.get(i);
      String type = obj.getClass().getSimpleName();

      if (type.equals("V1ServiceAccount")) {
        V1ServiceAccount serviceAccount = (V1ServiceAccount) obj;
        Optional<V1ObjectMeta> serviceAccountMetadata = Optional.ofNullable(serviceAccount)
            .map(V1ServiceAccount::getMetadata);
        coreV1Api.deleteNamespacedServiceAccount(serviceAccountMetadata.get().getName(),
            newNamespace, null, null, null, null,
            null, null);

      }
      if (type.equals("V1Secret")) {
        V1Secret secret = (V1Secret) obj;
        Optional<V1ObjectMeta> secretMetadata = Optional.ofNullable(secret).map(V1Secret::getMetadata);
        coreV1Api.deleteNamespacedSecret(secretMetadata.get().getName(),
            newNamespace, null, null, null, null, null,
            null);

      }
      if (type.equals("V1ConfigMap")) {
        coreV1Api.deleteNamespacedConfigMap("oauth2-proxy", newNamespace, null, null, null,
            null, null, null);

      }
      if (type.equals("V1Service")) {
        V1Service service = (V1Service) obj;
        Optional<V1ObjectMeta> serviceMetadata = Optional.ofNullable(service).map(V1Service::getMetadata);
        coreV1Api.deleteNamespacedService(serviceMetadata.get().getName(), newNamespace, null,
            null, null, null, null, null);

      }
      if (type.equals("V1Deployment")) {
        V1Deployment deployment = (V1Deployment) obj;
        Optional<V1ObjectMeta> deploymentMetadata = Optional.ofNullable(deployment)
            .map(V1Deployment::getMetadata);
        appsApi.deleteNamespacedDeployment(deploymentMetadata.get().getName(), newNamespace,
            null, null, null, null, null, null);

      }

    }

  }

  @Override
  public void deleteVirtualCluster(String bufferName, String provider, String region, String superCluster)
      throws KubectlException, IOException, ApiException, InterruptedException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, MinioException {
    ApiClient apiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);

    ModelMapper.addModelMap("tenancy.x-k8s.io", "v1alpha1", "VirtualCluster",
        "virtualclusters", true,
        V1VirtualCluster.class);
    Kubectl.delete(V1VirtualCluster.class).apiClient(apiClient).namespace("default")
        .name(bufferName).execute();

  }

  @Override
  public void deleteSubnet(String bufferName, String provider, String region, String superCluster)
      throws InternalError, IOException, ApiException, InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, InterruptedException, MinioException {
    DynamicKubernetesApi subnetsApi = apiClientManager.getSubnetApi(provider, region, superCluster);
    String namespaceName = getNamespaceNameWithBufferName(bufferName, provider, region, superCluster);
    subnetsApi.delete("subnet-" + namespaceName);

  }

  @Override
  public void deleteMachineDeployment(String bufferName, String provider, String region, String superCluster)
      throws IOException, KubectlException, ApiException, InterruptedException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, MinioException {
    ApiClient apiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);
    ModelMapper.addModelMap("cluster.k8s.io", "v1alpha1", "MachineDeployment",
        "machinedeployments", true,
        V1MachineDeployment.class);
    Kubectl.delete(V1MachineDeployment.class).apiClient(apiClient).namespace("kube-system")
        .name("md-" + bufferName).execute();
  }

  @Override
  public void deleteOrganizationLabelsFromSuperCluster(String nodeName, String provider, String region,
      String superCluster)
      throws IOException, KubectlException, ApiException, InterruptedException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, MinioException {
    ApiClient apiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);
    String patchString = "[{ \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1buffer-instance\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1organization\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1teamId\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1cloud-instance\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1region\" }]";
    V1Patch patch = new V1Patch(patchString);
    Kubectl.patch(V1Node.class).apiClient(apiClient).name(nodeName).patchContent(patch).execute();
  }

  @Override
  public void deleteWorkerLabelFromNode(String nodeName, String provider, String region, String superCluster)
      throws IOException, KubectlException, ApiException, InterruptedException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, MinioException {
    ApiClient apiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);
    String patchString = "[{ \"op\": \"remove\", \"path\": \"/metadata/labels/node-role.kubernetes.io~1worker\"}]";
    V1Patch patch = new V1Patch(patchString);
    Kubectl.patch(V1Node.class).apiClient(apiClient).name(nodeName).patchContent(patch).execute();
  }

  @Override
  public void deleteVirtualClusterNodes(String bufferName, String provider, String region, String superCluster)
      throws IOException, ApiException, InterruptedException, KubectlException, InvalidKeyException,
      NoSuchAlgorithmException, IllegalArgumentException, MinioException {
    ApiClient vcClient = getVirtualClusterClientWithBufferName(bufferName, provider, region, superCluster);
    CoreV1Api vcCoreV1Api = new CoreV1Api(vcClient);
    var nodeList = vcCoreV1Api.listNode(null, null, null, null, null, null, null, null, null, null);

    for (V1Node node : nodeList.getItems()) {
      Optional<String> nodeName = Optional.ofNullable(node).map(V1Node::getMetadata)
          .map(V1ObjectMeta::getName);
      if (!nodeName.get().equals(masterNodeName)) {
        Kubectl.delete(V1Node.class).apiClient(vcClient).name(nodeName.get())
            .execute();
      }
    }
  }

  @Override
  public String getTeamIdFromProcessId(String processId, String provider, String region, String superCluster)
      throws java.util.concurrent.ExecutionException, InterruptedException {
    String queryStr = "{roboticsCloud(where: {id: {equal: \"" + processId + "\"}}){teamId}}";
    Response response = graphqlClient.executeSync(queryStr);

    JsonObject data = response.getData();
    String teamId = data.getJsonArray("roboticsCloud").getJsonObject(0).getString("teamId");
    return teamId;
  }

  @Override
  public String convertJsonStringToYamlString(String jsonString)
      throws JsonProcessingException, IOException {
    // parse JSONgeneratedMachineDeploymentName
    JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
    // save it as YAML
    String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
    return jsonAsYaml;
  }

  @Override
  public String getNamespaceNameWithBufferName(String bufferName, String provider, String region, String superCluster)
      throws ApiException, InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException,
      InterruptedException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    String namespaceName = "";

    var namespaces = coreV1Api.listNamespace(null, null, null, null, null, null, null, null, null, null);
    for (int i = 0; i < namespaces.getItems().size(); i++) {
      Optional<String> nsName = Optional.ofNullable(namespaces.getItems().get(i).getMetadata())
          .map(V1ObjectMeta::getName);
      if (nsName.get().endsWith(bufferName)) {
        namespaceName = nsName.get();
        break;
      }
    }

    return namespaceName;
  }

  @Override
  public ApiClient getVirtualClusterClientWithBufferName(String bufferName, String provider, String region,
      String superCluster)
      throws IOException, ApiException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    DynamicKubernetesApi virtualClustersApi = apiClientManager.getVirtualClusterApi(provider, region, superCluster);
    String namespaceName = getNamespaceNameWithBufferName(bufferName, provider, region, superCluster);
    V1Secret sc = coreV1Api.readNamespacedSecret("admin-kubeconfig", namespaceName, null);
    Optional<Map<String, byte[]>> data = Optional.ofNullable(sc).map(V1Secret::getData);
    var certData = data.get().get("admin-kubeconfig");
    String cert = new String(certData, StandardCharsets.UTF_8);

    String nodePort = "";
    for (V1Service service : coreV1Api
        .listNamespacedService(namespaceName, null, null, null, null, null, null, null,
            null, null, null)
        .getItems()) {
      Optional<String> svcName = Optional.ofNullable(service).map(V1Service::getMetadata)
          .map(V1ObjectMeta::getName);
      Optional<Integer> optionalNodePort = Optional.ofNullable(service).map(m -> m.getSpec()).map(m -> m.getPorts())
          .map(m -> m.get(0))
          .map(m -> m.getNodePort());
      if (svcName.get().equals("apiserver-svc")) {
        nodePort = optionalNodePort.get().toString();
        break;
      }
    }
    String masterIP = "";
    V1NodeList nodes = coreV1Api.listNode(null, null, null, null, "node-role.kubernetes.io/master", null,
        null, null, null, null);
    Optional<List<V1NodeAddress>> optionalAddresses = Optional.ofNullable(nodes.getItems().get(0).getStatus())
        .map(m -> m.getAddresses());
    for (var address : optionalAddresses.get()) {
      if (address.getType().equals("ExternalIP")) {
        masterIP = address.getAddress();
        break;
      }
    }

    System.out.println("ExternalIP: " + "https://" + masterIP + ":" + nodePort);

    while (!virtualClustersApi.get("default", bufferName).getObject().getRaw().get("status")
        .getAsJsonObject()
        .get("phase").getAsString().equals("Running")) {
      Thread.sleep(3000);
    }
    org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
    var yml = yaml.load(cert);
    ObjectMapper mapper = new ObjectMapper();
    var result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(yml);

    JsonNode actualObj = mapper.readTree(result);

    String clientCertificateData = actualObj.get("users").get(0).get("user").get("client-certificate-data")
        .toString()
        .replace("\"", "");
    byte[] byteClientCertData = Base64.getDecoder().decode(clientCertificateData.getBytes("UTF-8"));

    String clientKeyData = actualObj.get("users").get(0).get("user").get("client-key-data").toString()
        .replace("\"",
            "");

    byte[] byteClientKeyData = Base64.getDecoder().decode(clientKeyData.getBytes("UTF-8"));

    String certificateAuthorityData = actualObj.get("clusters").get(0).get("cluster").get(
        "certificate-authority-data")
        .toString().replace("\"", "");

    byte[] byteCertificateAuthData = Base64.getDecoder().decode(certificateAuthorityData.getBytes("UTF-8"));

    String basePath = "https://" + masterIP + ":" + nodePort;
    /* Virtual Cluster Client */
    ApiClient newClient = new ClientBuilder().setBasePath(basePath)
        .setAuthentication(new ClientCertificateAuthentication(byteClientCertData,
            byteClientKeyData))
        .setCertificateAuthority(byteCertificateAuthData)
        .setVerifyingSsl(true)
        .build();
    return newClient;
  }

  public Boolean doesCloudInstanceExist(Organization organization, String teamId,
      String cloudInstanceName, String provider, String region, String superCluster)
      throws ExecutionException, InterruptedException {
    return false;
    // String queryStr = "{roboticsCloud(where: {and: [{organization: {name: {equal:
    // \"" + organization.getName()
    // + "\"}}},{teamId: {equal:\"" + teamId + "\"}}, {cloudInstanceName: {equal:
    // \"" + cloudInstanceName
    // + "\"}}]}) {id}}";

    // Response response = graphqlClient.executeSync(queryStr);

    // javax.json.JsonObject data = response.getData();
    // if (data.getJsonArray("roboticsCloud").size() > 0) {
    // return true;
    // }
    // return false;

  }

  @Override
  public ApiClient userApiClient(String bufferName, String token, String provider, String region, String superCluster)
      throws IOException, ApiException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, MinioException {
    CoreV1Api coreV1Api = apiClientManager.getCoreApi(provider, region, superCluster);
    DynamicKubernetesApi virtualClustersApi = apiClientManager.getVirtualClusterApi(provider, region, superCluster);
    String namespaceName = getNamespaceNameWithBufferName(bufferName, provider, region, superCluster);
    System.out.println("Namespace Name: " + namespaceName);
    V1Secret sc = coreV1Api.readNamespacedSecret("admin-kubeconfig", namespaceName, null);
    Optional<Map<String, byte[]>> optionalData = Optional.ofNullable(sc.getData());
    var certData = optionalData.get().get("admin-kubeconfig");
    String cert = new String(certData, StandardCharsets.UTF_8);

    String nodePort = "";
    for (V1Service service : coreV1Api
        .listNamespacedService(namespaceName, null, null, null, null, null, null, null,
            null, null, null)
        .getItems()) {
      Optional<String> svcName = Optional.ofNullable(service.getMetadata()).map(m -> m.getName());
      Optional<Integer> svcPort = Optional.ofNullable(service).map(V1Service::getSpec).map(m -> m.getPorts())
          .map(m -> m.get(0)).map(m -> m.getNodePort());
      if (svcName.get().equals("apiserver-svc")) {
        nodePort = svcPort.get().toString();
        break;
      }
    }
    String masterIP = "";
    V1NodeList nodes = coreV1Api.listNode(null, null, null, null, "node-role.kubernetes.io/master", null,
        null, null, null, null);
    Optional<List<V1NodeAddress>> optionalAddresses = Optional.ofNullable(nodes.getItems().get(0).getStatus())
        .map(m -> m.getAddresses());
    for (var address : optionalAddresses.get()) {
      if (address.getType().equals("ExternalIP")) {
        masterIP = address.getAddress();
        break;
      }
    }

    System.out.println("ExternalIP: " + "https://" + masterIP + ":" + nodePort);

    while (!virtualClustersApi.get("default", bufferName).getObject().getRaw().get("status")
        .getAsJsonObject()
        .get("phase").getAsString().equals("Running")) {
      Thread.sleep(3000);
    }
    org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
    var yml = yaml.load(cert);
    ObjectMapper mapper = new ObjectMapper();
    var result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(yml);

    JsonNode actualObj = mapper.readTree(result);

    String certificateAuthorityData = actualObj.get("clusters").get(0).get("cluster").get(
        "certificate-authority-data")
        .toString().replace("\"", "");

    byte[] byteCertificateAuthData = Base64.getDecoder().decode(certificateAuthorityData.getBytes("UTF-8"));

    String basePath = "https://" + masterIP + ":" + nodePort;
    /* Virtual Cluster Client */
    ApiClient newClient = new ClientBuilder().setBasePath(basePath)
        .setAuthentication(new AccessTokenAuthentication(token))
        .setCertificateAuthority(byteCertificateAuthData)
        .setVerifyingSsl(true)
        .build();
    return newClient;
  }

  @Override
  public ApiClient adminApiClient(String provider, String region, String superCluster)
      throws IOException, ApiException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, MinioException {
    String clusterName = provider + "/" + region + "/" + superCluster + "/" + "kubeconfig.yaml";
    Artifact artifact = new Artifact();
    artifact.setName(clusterName);
    com.google.gson.JsonObject object = storageRepository.getYamlTemplate(artifact, "providers");

    String kubernetesServerUrl = object.get("clusters").getAsJsonArray().get(0).getAsJsonObject().get("cluster")
        .getAsJsonObject()
        .get("server").getAsString();

    String certificateAuthorityData = object.get("clusters").getAsJsonArray().get(0).getAsJsonObject().get("cluster")
        .getAsJsonObject()
        .get("certificate-authority-data").getAsString();
    byte[] byteCertificateAuthData = Base64.getDecoder().decode(certificateAuthorityData.getBytes("UTF-8"));

    String kubernetesServerCsData = object.get("users").getAsJsonArray().get(0).getAsJsonObject().get("user")
        .getAsJsonObject()
        .get("client-certificate-data").getAsString();
    String clientCertificateData = kubernetesServerCsData;
    byte[] byteClientCertData = Base64.getDecoder().decode(clientCertificateData.getBytes("UTF-8"));

    String kubernetesServerCkData = object.get("users").getAsJsonArray().get(0).getAsJsonObject().get("user")
        .getAsJsonObject()
        .get("client-key-data").getAsString();
    String clientKeyData = kubernetesServerCkData;
    byte[] byteClientKeyData = Base64.getDecoder().decode(clientKeyData.getBytes("UTF-8"));

    System.out.println("creating vc");
    /* Virtual Cluster Client */
    ApiClient newClient = new ClientBuilder().setBasePath(kubernetesServerUrl)
        .setAuthentication(new ClientCertificateAuthentication(byteClientCertData, byteClientKeyData))
        .setCertificateAuthority(byteCertificateAuthData)
        .setVerifyingSsl(true)
        .build();

    VCCreated++;
    System.out.println("Returning new VC! --- " + VCCreated);
    return newClient;
  }

  @Override
  public String getAvailableCIDRBlock(String provider, String region, String superCluster)
      throws InvalidKeyException, NoSuchAlgorithmException, IllegalArgumentException, IOException, ApiException,
      InterruptedException, MinioException {
    DynamicKubernetesApi subnetApi = apiClientManager.getSubnetApi(provider, region, superCluster);
    List<DynamicKubernetesObject> subnetList = subnetApi.list().getObject().getItems();
    int counter = 1;
    while (counter < 255) {
      String subnetIP = "10.10." + counter + ".0/24";
      if (subnetList.stream()
          .anyMatch(subnet -> subnet.getRaw().get("spec").getAsJsonObject()
              .get("cidrBlock").getAsString().equals(subnetIP))) {
        counter++;
      } else {
        return subnetIP;
      }
    }
    return null;
  }

}
