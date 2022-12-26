package org.robolaunch.repository.concretes;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import io.kubernetes.client.util.generic.options.ListOptions;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

@ApplicationScoped
public class CloudInstanceHelperRepositoryImpl implements CloudInstanceHelperRepository {
  private DynamicKubernetesApi machinesApi;
  private CoreV1Api coreV1Api;
  private AppsV1Api appsApi;
  private DynamicKubernetesApi virtualClustersApi;
  private DynamicKubernetesApi subnetsApi;
  private DynamicKubernetesApi clusterVersionApi;

  private DynamicGraphQLClient graphqlClient;

  @ConfigProperty(name = "backend.url")
  String backendUrl;
  @ConfigProperty(name = "dns.zone")
  String dnsZoneName;
  @ConfigProperty(name = "kogito.dataindex.http.url")
  String kogitoDataIndexUrl;
  @ConfigProperty(name = "kubernetes.server")
  String kubernetesServerUrl;
  @ConfigProperty(name = "kubernetes.server.caData")
  String kubernetesServerCaData;
  @ConfigProperty(name = "kubernetes.server.csData")
  String kubernetesServerCsData;
  @ConfigProperty(name = "kubernetes.server.ckData")
  String kubernetesServerCkData;
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

  @PostConstruct
  public void initializeApis() throws IOException, ApiException, InterruptedException {
    ApiClient apiClient = adminApiClient();

    this.machinesApi = new DynamicKubernetesApi("cluster.k8s.io",
        "v1alpha1", "machines",
        apiClient);
    this.clusterVersionApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
        "clusterversions", apiClient);
    this.coreV1Api = new CoreV1Api(apiClient);
    this.virtualClustersApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
        "virtualclusters", apiClient);
    this.subnetsApi = new DynamicKubernetesApi("kubeovn.io", "v1",
        "subnets", apiClient);
    this.appsApi = new AppsV1Api(apiClient);
    graphqlClient = DynamicGraphQLClientBuilder.newBuilder()
        .url(kogitoDataIndexUrl + "/graphql/")
        .build();

  }

  @Override
  public String getCloudInstanceIP(String bufferName) throws ApiException {
    String namespaceName = getNamespaceNameWithBufferName(bufferName);
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
  public void bufferCall(String instanceType) throws IOException {
    URL url = new URL(backendUrl + "/bufferCloudInstance");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);
    String input = "{\"instanceType\": \"" + instanceType + "\"}";

    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
    wr.write(input.getBytes());

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String line;
    String result = "";
    while ((line = bufferedReader.readLine()) != null) {
      result += line;
    }
    wr.close();
  }

  @Override
  public String generateBufferName() {
    RandomGenerator randomGenerator = new RandomGeneratorImpl();
    String randomString = randomGenerator.generateRandomString(8);
    return randomString.toLowerCase();
  }

  @Override
  public void CIOperationCall(String processId, String operation) throws IOException {
    URL url = new URL(backendUrl + "/initializeRoboticsCloud/" + processId + "/operation");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);

    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
    String jsonInputString = "{\"operation\": \"" + operation + "\"}";
    wr.write(jsonInputString.getBytes());

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String line;
    String result = "";
    while ((line = bufferedReader.readLine()) != null) {
      result += line;
    }
    wr.close();
  }

  @Override
  public String getGeneratedMachineName(String bufferName) {
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
  public Boolean nodeRefChecker(String bufferName, String machineName) throws ApiException {
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
  public Boolean isVirtualClusterReady(String bufferName) {
    String cloudInstanceName = "vc-" + bufferName;

    ListOptions listOptions = new ListOptions();
    listOptions.setLabelSelector("robolaunch.io/buffer-instance=" + bufferName);
    var vcs = virtualClustersApi.list(listOptions);
    for (var vc : vcs.getObject().getItems()) {
      Optional<String> vcName = Optional.ofNullable(vc).map(DynamicKubernetesObject::getMetadata)
          .map(m -> m.getName());
      if (vcName.get().equals(cloudInstanceName)) {
        if (vc.getRaw().get("status").getAsJsonObject().get("phase").getAsString()
            .equals("Running")) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Integer getBufferingVirtualClusterCount(String instanceType) throws InterruptedException, ExecutionException {
    String queryStr = "{BufferCloudInstance(where: {and: [{status: {equal: \"Creating\"}},{instanceType: {equal:\""
        + instanceType + "\"}}]}){id}}";

    Response response = graphqlClient.executeSync(queryStr);

    javax.json.JsonObject data = response.getData();
    if (data != null) {
      if (data.get("BufferCloudInstance") != null) {
        return data.get("BufferCloudInstance").asJsonArray().size();
      } else {
        return 0;
      }
    } else {
      return 0;
    }

  }

  @Override
  public Integer getBufferedVirtualClusterCount(String instanceType) {
    ListOptions listOptions = new ListOptions();
    listOptions.setLabelSelector(
        "buffered=true, !robolaunch.io/organization, robolaunch.io/instance-type=" + instanceType);
    var vcs = virtualClustersApi.list(listOptions).getObject().getItems();
    Integer counter = 0;
    for (DynamicKubernetesObject vc : vcs) {
      if (vc.getRaw().get("status").getAsJsonObject().get("phase").getAsString().equals("Running")) {
        counter++;
      }
    }
    return counter;
  }

  @Override
  public String selectBufferedVirtualCluster(Integer vcCount)
      throws KubectlException, IOException, InterruptedException {
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
    return bufferName;

  }

  @Override
  public String getNodeName(String machineName) {
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
  public Boolean isStatefulSetsUp(String namespaceName) throws ApiException, IOException {
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
  public Boolean isStatefulSetsDown(String namespaceName) throws ApiException {
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
  public Boolean isCoreDNSDeploymentUp(String bufferName) throws IOException, ApiException, InterruptedException {
    ApiClient vcClient = getVirtualClusterClientWithBufferName(bufferName);

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
  public Boolean isCertManagerReady(String bufferName)
      throws ApiException, IOException, InterruptedException {
    Boolean podsReady = false;
    Boolean svcReady = false;

    ApiClient apiClient = getVirtualClusterClientWithBufferName(bufferName);
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
  public String selectNode(String bufferName)
      throws ApiException, KubectlException, IOException, InterruptedException {
    ApiClient apiClient = adminApiClient();

    while (true) {
      V1NodeList nodeList = coreV1Api.listNode(null, null, null, null,
          "!node-role.kubernetes.io/master, !robolaunch.io/buffer-instance, node-role.kubernetes.io/worker=worker",
          null, null, null, null, null);
      for (V1Node node : nodeList.getItems()) {
        Optional<V1ObjectMeta> nodeMetadata = Optional.ofNullable(node).map(V1Node::getMetadata);
        if (!isNodeReady(nodeMetadata.get().getName())
            && isNodeUnschedulable(nodeMetadata.get().getName())) {
          if (amazonRepository.getInstanceState(nodeMetadata.get().getName())
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
  public Boolean isNodeUnschedulable(String nodeName) throws ApiException {
    var node = coreV1Api.readNode(nodeName, null);
    Optional<V1NodeSpec> nodeSpec = Optional.ofNullable(node).map(V1Node::getSpec);
    if (nodeSpec.get().getUnschedulable() == null) {
      return false;
    }
    var unschedulable = nodeSpec.get().getUnschedulable();

    return unschedulable;
  }

  @Override
  public Boolean isNodeReady(String nodeName) throws ApiException {
    var node = coreV1Api.readNode(nodeName, null);
    Optional<List<V1NodeCondition>> nodeConditions = Optional.ofNullable(node).map(V1Node::getStatus)
        .map(m -> m.getConditions());
    var lastCondition = nodeConditions.get()
        .get(nodeConditions.get().size() - 1);
    return lastCondition.getType().equals("Ready") && lastCondition
        .getStatus().equals("True");
  }

  @Override
  public Boolean isSubnetUsed(String bufferName) throws ApiException {
    String namespaceName = getNamespaceNameWithBufferName(bufferName);

    var subnet = subnetsApi.get("subnet-" + namespaceName).getObject().getRaw();
    return subnet.getAsJsonObject().get("status").getAsJsonObject().get("v4usingIPs").getAsInt() > 0;
  }

  @Override
  public String findNode(String bufferName, Organization organization, String teamId,
      String cloudInstanceName) throws ApiException {
    V1NodeList nodeList = coreV1Api.listNode(null, null, null, null,
        "!node-role.kubernetes.io/master, robolaunch.io/organization=" + organization.getName()
            + ", robolaunch.io/team=" + teamId
            + ", robolaunch.io/cloud-instance=" + cloudInstanceName,
        null, null, null, null, null);
    Optional<V1ObjectMeta> nodeMetadata = Optional.ofNullable(nodeList.getItems().get(0)).map(V1Node::getMetadata);
    return nodeMetadata.get().getName();
  }

  @Override
  public Boolean isMachineCreated(String bufferName) {
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
      String nodeName) {
    Boolean isInstanceHealthy = amazonRepository.isRunning(nodeName);
    return isInstanceHealthy;
  }

  @Override
  public void deleteDNSRecord(Organization organization, String nodeName)
      throws ApiException, InternalError, ApplicationException, IOException {
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
  public void deleteClusterVersion(String bufferName) {
    clusterVersionApi.delete("cv-" + bufferName);
  }

  @Override
  public void deleteOAuth2ProxyResources(String bufferName)
      throws ApiException, InvalidKeyException, ErrorResponseException, InsufficientDataException,
      InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException,
      XmlParserException, IllegalArgumentException, IOException, InterruptedException {
    String namespaceName = getNamespaceNameWithBufferName(bufferName);

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
  public void deleteVirtualCluster(String bufferName)
      throws KubectlException, IOException, ApiException, InterruptedException {
    ModelMapper.addModelMap("tenancy.x-k8s.io", "v1alpha1", "VirtualCluster",
        "virtualclusters", true,
        V1VirtualCluster.class);
    ApiClient apiClient = adminApiClient();
    Kubectl.delete(V1VirtualCluster.class).apiClient(apiClient).namespace("default")
        .name("vc-" + bufferName).execute();

  }

  @Override
  public void deleteSubnet(String bufferName)
      throws InternalError, IOException, ApiException {
    String namespaceName = getNamespaceNameWithBufferName(bufferName);
    subnetsApi.delete("subnet-" + namespaceName);

  }

  @Override
  public void deleteMachineDeployment(String bufferName)
      throws IOException, KubectlException, ApiException, InterruptedException {
    ModelMapper.addModelMap("cluster.k8s.io", "v1alpha1", "MachineDeployment",
        "machinedeployments", true,
        V1MachineDeployment.class);
    ApiClient apiClient = adminApiClient();
    Kubectl.delete(V1MachineDeployment.class).apiClient(apiClient).namespace("kube-system")
        .name("md-" + bufferName).execute();
  }

  @Override
  public void deleteOrganizationLabelsFromSuperCluster(String nodeName)
      throws IOException, KubectlException, ApiException, InterruptedException {
    ApiClient apiClient = adminApiClient();
    String patchString = "[{ \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1buffer-instance\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1organization\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1department\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1cloud-instance\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1region\" }]";
    V1Patch patch = new V1Patch(patchString);
    Kubectl.patch(V1Node.class).apiClient(apiClient).name(nodeName).patchContent(patch).execute();
  }

  @Override
  public void deleteWorkerLabelFromNode(String nodeName)
      throws IOException, KubectlException, ApiException, InterruptedException {
    ApiClient apiClient = adminApiClient();
    String patchString = "[{ \"op\": \"remove\", \"path\": \"/metadata/labels/node-role.kubernetes.io~1worker\"}]";
    V1Patch patch = new V1Patch(patchString);
    Kubectl.patch(V1Node.class).apiClient(apiClient).name(nodeName).patchContent(patch).execute();
  }

  @Override
  public void deleteVirtualClusterNodes(String bufferName)
      throws IOException, ApiException, InterruptedException, KubectlException {
    ApiClient vcClient = getVirtualClusterClientWithBufferName(bufferName);
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
  public String getTeamIdFromProcessId(String processId)
      throws java.util.concurrent.ExecutionException, InterruptedException {
    String queryStr = "{InitializeRoboticsCloud(where: {id: {equal: \"" + processId + "\"}}){teamId}}";
    Response response = graphqlClient.executeSync(queryStr);

    JsonObject data = response.getData();
    String teamId = data.getJsonArray("InitializeRoboticsCloud").getJsonObject(0).getString("teamId");
    return teamId;
  }

  @Override
  public String convertJsonStringToYamlString(String jsonString) throws JsonProcessingException, IOException {
    // parse JSONgeneratedMachineDeploymentName
    JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
    // save it as YAML
    String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
    return jsonAsYaml;
  }

  @Override
  public String getNamespaceNameWithBufferName(String bufferName) throws ApiException {
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
  public ApiClient getVirtualClusterClientWithBufferName(String bufferName)
      throws IOException, ApiException, InterruptedException {
    String namespaceName = getNamespaceNameWithBufferName(bufferName);
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

    String generatedcloudInstanceName = "vc-" + bufferName;

    while (!virtualClustersApi.get("default", generatedcloudInstanceName).getObject().getRaw().get("status")
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
      String cloudInstanceName) throws ExecutionException, InterruptedException {
    String queryStr = "{InitializeRoboticsCloud(where: {and: [{organization: {name: {equal: \"" + organization.getName()
        + "\"}}},{teamId: {equal:\"" + teamId + "\"}}, {cloudInstanceName: {equal: \"" + cloudInstanceName
        + "\"}}]}) {id}}";

    Response response = graphqlClient.executeSync(queryStr);

    javax.json.JsonObject data = response.getData();
    if (data.getJsonArray("InitializeRoboticsCloud").size() > 0) {
      return true;
    }
    return false;

  }

  @Override
  public ApiClient adminApiClient()
      throws IOException, ApiException, InterruptedException {

    String certificateAuthorityData = kubernetesServerCaData;
    byte[] byteCertificateAuthData = Base64.getDecoder().decode(certificateAuthorityData.getBytes("UTF-8"));

    String clientCertificateData = kubernetesServerCsData;
    byte[] byteClientCertData = Base64.getDecoder().decode(clientCertificateData.getBytes("UTF-8"));

    String clientKeyData = kubernetesServerCkData;
    byte[] byteClientKeyData = Base64.getDecoder().decode(clientKeyData.getBytes("UTF-8"));

    /* Virtual Cluster Client */
    ApiClient newClient = new ClientBuilder().setBasePath(kubernetesServerUrl)
        .setAuthentication(new ClientCertificateAuthentication(byteClientCertData, byteClientKeyData))
        .setCertificateAuthority(byteCertificateAuthData)
        .setVerifyingSsl(true)
        .build();
    return newClient;
  }

  @Override
  public ApiClient userApiClient(String bufferName, String token)
      throws IOException, ApiException, InterruptedException {
    String namespaceName = getNamespaceNameWithBufferName(bufferName);
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

    String generatedcloudInstanceName = "vc-" + bufferName;

    while (!virtualClustersApi.get("default", generatedcloudInstanceName).getObject().getRaw().get("status")
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
}
