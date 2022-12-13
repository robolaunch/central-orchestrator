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
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
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
  public void initializeApis() throws IOException {
    ApiClient apiClient = ClientBuilder.standard().build();
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
      if (service.getMetadata().getName().equals("apiserver-svc")) {
        nodePort = service.getSpec().getPorts().get(0).getNodePort().toString();
        break;
      }
    }
    String masterIP = "";
    V1NodeList nodes = coreV1Api.listNode(null, null, null, null, "node-role.kubernetes.io/master", null,
        null, null, null, null);
    for (var address : nodes.getItems().get(0).getStatus().getAddresses()) {
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
    // connection.setRequestProperty("Content-Type",
    // "application/x-www-form-urlencoded; charset=utf-8");
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
    // connection.setRequestProperty("Content-Type",
    // "application/x-www-form-urlencoded; charset=utf-8");
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
    System.out.println("Result: " + result);
    wr.close();
  }

  @Override
  public String getGeneratedMachineName(String bufferName) {
    String machineName = "";
    var list = machinesApi.list("kube-system").getObject().getItems();
    Iterator<DynamicKubernetesObject> iterator = list.iterator();
    while (iterator.hasNext()) {
      DynamicKubernetesObject obj = iterator.next();
      if (obj.getMetadata().getName()
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

      if (node.getStatus().getConditions().get(
          node.getStatus()
              .getConditions().size()
              - 1)
          .getType().equals("Ready")
          && node.getStatus().getConditions().get(
              node.getStatus()
                  .getConditions()
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
    var vcs = virtualClustersApi.list("default", listOptions);
    for (var vc : vcs.getObject().getItems()) {
      if (vc.getMetadata().getName().equals(cloudInstanceName)) {
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
    String bufferName = vcs.getObject().getItems().get(randomInteger).getMetadata().getLabels()
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
      if (pod.getMetadata().getName().equals("etcd-0")) {
        if (pod.getStatus().getPhase().equals("Running")) {
          if (pod.getStatus().getContainerStatuses().get(0).getReady()) {
            etcdReady = true;
          }
        }
      }
      if (pod.getMetadata().getName().equals("apiserver-0")) {
        if (pod.getStatus().getPhase().equals("Running")) {
          if (pod.getStatus().getContainerStatuses().get(0).getReady()) {
            apiServerReady = true;
          }
        }

      }
      if (pod.getMetadata().getName().equals("controller-manager-0")) {
        if (pod.getStatus().getPhase().equals("Running")) {
          if (pod.getStatus().getContainerStatuses().get(0).getReady()) {
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
      if (pod.getMetadata().getName().equals("etcd-0") || pod.getMetadata().getName()
          .equals("controller-manager-0")
          || pod.getMetadata().getName()
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
      if (deployment.getMetadata().getName().equals("coredns")) {
        if (deployment.getStatus().getReadyReplicas() != null) {
          System.out.println(
              "Ready replicas: " + deployment.getStatus().getReadyReplicas());
          if (deployment.getStatus().getReadyReplicas() >= 1) {
            return true;
          }
        } else {
          System.out.println("Ready replicas is null");
        }

      }
    }
    return false;
  }

  @Override
  public Boolean isCertManagerReady(String namespaceName) throws ApiException, IOException {
    V1PodList podList = coreV1Api.listNamespacedPod(namespaceName + "-cert-manager", null, null, null, null,
        null, null, null, null, null, null);
    for (V1Pod pod : podList.getItems()) {
      if (!pod.getStatus().getPhase().equals("Running")) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String selectNode(String bufferName)
      throws ApiException, KubectlException, IOException, InterruptedException {
    ApiClient apiClient = Config.defaultClient();

    while (true) {
      V1NodeList nodeList = coreV1Api.listNode(null, null, null, null,
          "!node-role.kubernetes.io/master, !robolaunch.io/buffer-instance, node-role.kubernetes.io/worker=worker",
          null, null, null, null, null);
      for (V1Node node : nodeList.getItems()) {

        if (!isNodeReady(node.getMetadata().getName())
            && isNodeUnschedulable(node.getMetadata().getName())) {
          if (amazonRepository.getInstanceState(node.getMetadata().getName())
              .equals("stopped")) {

            Kubectl.label(V1Node.class).name(node.getMetadata().getName())
                .apiClient(apiClient)
                .addLabel("robolaunch.io/buffer-instance", bufferName)
                .execute();
            System.out.println("Node " + node.getMetadata().getName() + " is labeled");
            return node.getMetadata().getName();
          }
        }

      }
      System.out.println("No available node found.");
      Thread.sleep(4000);
    }

  }

  @Override
  public Boolean isNodeUnschedulable(String nodeName) throws ApiException {
    var node = coreV1Api.readNode(nodeName, null);
    if (node.getSpec().getUnschedulable() == null) {
      return false;
    }
    var unschedulable = node.getSpec().getUnschedulable();

    return unschedulable;
  }

  @Override
  public Boolean isNodeReady(String nodeName) throws ApiException {
    var node = coreV1Api.readNode(nodeName, null);
    var lastCondition = node.getStatus().getConditions()
        .get(node.getStatus().getConditions().size() - 1);
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
  public String findNode(String bufferName, Organization organization, String departmentName,
      String cloudInstanceName) throws ApiException {
    V1NodeList nodeList = coreV1Api.listNode(null, null, null, null,
        "!node-role.kubernetes.io/master, robolaunch.io/organization=" + organization.getName()
            + ", robolaunch.io/department=" + departmentName
            + ", robolaunch.io/cloud-instance=" + cloudInstanceName,
        null, null, null, null, null);
    return nodeList.getItems().get(0).getMetadata().getName();
  }

  @Override
  public Boolean isMachineCreated(String bufferName) {
    var machines = machinesApi.list();
    for (var machine : machines.getObject().getItems()) {
      if (machine.getMetadata().getName().contains(bufferName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean healthCheck(Organization organization, String departmentName, String cloudInstanceName,
      String nodeName) {
    Boolean isInstanceHealthy = amazonRepository.isRunning(nodeName);
    return isInstanceHealthy;
  }

  @Override
  public void deleteDNSRecord(Organization organization, String nodeName)
      throws ApiException, InternalError, ApplicationException, IOException {
    String externalIP = "";
    var node = coreV1Api.readNode(nodeName, null);
    var addresses = node.getStatus().getAddresses();
    for (int i = 0; i < addresses.size(); i++) {
      if (addresses.get(i).getType().equals(
          "ExternalIP")) {
        externalIP = addresses.get(i).getAddress().toString();
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
        coreV1Api.deleteNamespacedServiceAccount(serviceAccount.getMetadata().getName(),
            newNamespace, null, null, null, null,
            null, null);

      }
      if (type.equals("V1Secret")) {
        V1Secret secret = (V1Secret) obj;
        coreV1Api.deleteNamespacedSecret(secret.getMetadata().getName(),
            newNamespace, null, null, null, null, null,
            null);

      }
      if (type.equals("V1ConfigMap")) {
        coreV1Api.deleteNamespacedConfigMap("oauth2-proxy", newNamespace, null, null, null,
            null, null, null);

      }
      if (type.equals("V1Service")) {
        V1Service service = (V1Service) obj;
        coreV1Api.deleteNamespacedService(service.getMetadata().getName(), newNamespace, null,
            null, null, null, null, null);

      }
      if (type.equals("V1Deployment")) {
        V1Deployment deployment = (V1Deployment) obj;
        appsApi.deleteNamespacedDeployment(deployment.getMetadata().getName(), newNamespace,
            null, null, null, null, null, null);

      }

    }

  }

  @Override
  public void deleteVirtualCluster(String bufferName) throws KubectlException, IOException {
    ModelMapper.addModelMap("tenancy.x-k8s.io", "v1alpha1", "VirtualCluster",
        "virtualclusters", true,
        V1VirtualCluster.class);
    ApiClient apiClient = Config.defaultClient();
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
  public void deleteMachineDeployment(String bufferName) throws IOException, KubectlException {
    ModelMapper.addModelMap("cluster.k8s.io", "v1alpha1", "MachineDeployment",
        "machinedeployments", true,
        V1MachineDeployment.class);
    ApiClient apiClient = Config.defaultClient();
    Kubectl.delete(V1MachineDeployment.class).apiClient(apiClient).namespace("kube-system")
        .name("md-" + bufferName).execute();
  }

  @Override
  public void deleteOrganizationLabelsFromSuperCluster(String nodeName) throws IOException, KubectlException {
    ApiClient apiClient = Config.defaultClient();
    String patchString = "[{ \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1buffer-instance\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1organization\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1department\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1cloud-instance\" }, { \"op\": \"remove\", \"path\": \"/metadata/labels/robolaunch.io~1super-cluster\" }]";
    V1Patch patch = new V1Patch(patchString);
    Kubectl.patch(V1Node.class).apiClient(apiClient).name(nodeName).patchContent(patch).execute();
  }

  @Override
  public void deleteWorkerLabelFromNode(String nodeName) throws IOException, KubectlException {
    ApiClient apiClient = Config.defaultClient();
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
      if (!node.getMetadata().getName().equals("ip-172-31-180-150.eu-central-1.compute.internal")) {
        Kubectl.delete(V1Node.class).apiClient(vcClient).name(node.getMetadata().getName())
            .execute();
        System.out.println("Node deleted: " + node.getMetadata().getName());
      }
    }
  }

  @Override
  public String getTeamIdFromProcessId(String processId)
      throws java.util.concurrent.ExecutionException, InterruptedException {
    String queryStr = "{InitializeRoboticsCloud(where: {id: {equal: \"" + processId + "\"}}){departmentName}}";
    System.out.println("Query: " + queryStr);
    Response response = graphqlClient.executeSync(queryStr);

    JsonObject data = response.getData();
    String teamId = data.getJsonArray("InitializeRoboticsCloud").getJsonObject(0).getString("departmentName");
    System.out.println("TeamId: " + teamId);
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
      if (namespaces.getItems().get(i).getMetadata().getName().endsWith(bufferName)) {
        namespaceName = namespaces.getItems().get(i).getMetadata().getName();
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
    var certData = sc.getData().get("admin-kubeconfig");
    String cert = new String(certData, StandardCharsets.UTF_8);

    String nodePort = "";
    for (V1Service service : coreV1Api
        .listNamespacedService(namespaceName, null, null, null, null, null, null, null,
            null, null, null)
        .getItems()) {
      if (service.getMetadata().getName().equals("apiserver-svc")) {
        nodePort = service.getSpec().getPorts().get(0).getNodePort().toString();
        break;
      }
    }
    String masterIP = "";
    V1NodeList nodes = coreV1Api.listNode(null, null, null, null, "node-role.kubernetes.io/master", null,
        null, null, null, null);
    for (var address : nodes.getItems().get(0).getStatus().getAddresses()) {
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
        + "\"}}},{departmentName: {equal:\"" + teamId + "\"}}, {cloudInstanceName: {equal: \"" + cloudInstanceName
        + "\"}}]}) {id}}";

    Response response = graphqlClient.executeSync(queryStr);

    javax.json.JsonObject data = response.getData();
    if (data.getJsonArray("InitializeRoboticsCloud").size() > 0) {
      return true;
    }
    return false;

  }

  public void createCRB(String bufferName) throws IOException, ApiException, InterruptedException, InvalidKeyException,
      ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException,
      NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException {
    ApiClient vcClient = getVirtualClusterClientWithBufferName(bufferName);
    RbacAuthorizationV1Api rbacApi = new RbacAuthorizationV1Api(vcClient);

    Artifact artifact = new Artifact();
    artifact.setName("clusterAdminRole.yaml");
    String bucket = "template-artifacts";
    String yaml = storageRepository.getContent(artifact, bucket);
    List<Object> list = Yaml.loadAll(yaml);

    for (int i = 0; i < list.size(); i++) {
      Object obj = list.get(i);
      String type = obj.getClass().getSimpleName();
      if (type.equals("V1ClusterRole")) {
        V1ClusterRole clusterRole = (V1ClusterRole) obj;
        rbacApi.createClusterRole(clusterRole, null, null, null, null);
      }
      if (type.equals("V1ClusterRoleBinding")) {
        V1ClusterRoleBinding clusterRoleBinding = (V1ClusterRoleBinding) obj;
        String username = jwt.getClaim("preferred_username");
        System.out.println("Username: " + username);
        clusterRoleBinding.getSubjects().get(0).setName("org-whilst-dep-syrs9ovd");
        rbacApi.createClusterRoleBinding(clusterRoleBinding, null, null, null,
            null);
      }
    }
  }

  @Override
  public ApiClient customApiClient(String bufferName)
      throws IOException, ApiException, InterruptedException {
    String namespaceName = getNamespaceNameWithBufferName(bufferName);

    String nodePort = "";
    for (V1Service service : coreV1Api
        .listNamespacedService(namespaceName, null, null, null, null, null, null, null,
            null, null, null)
        .getItems()) {
      if (service.getMetadata().getName().equals("apiserver-svc")) {
        nodePort = service.getSpec().getPorts().get(0).getNodePort().toString();
        break;
      }
    }
    String masterIP = "";
    V1NodeList nodes = coreV1Api.listNode(null, null, null, null, "node-role.kubernetes.io/master", null,
        null, null, null, null);
    for (var address : nodes.getItems().get(0).getStatus().getAddresses()) {
      if (address.getType().equals("ExternalIP")) {
        masterIP = address.getAddress();
        break;
      }
    }
    String url = "https://" + masterIP + ":" + nodePort;

    V1Secret sc = coreV1Api.readNamespacedSecret("admin-kubeconfig", namespaceName, null);
    var certData = sc.getData().get("admin-kubeconfig");
    String cert = new String(certData, StandardCharsets.UTF_8);

    org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
    var yml = yaml.load(cert);
    ObjectMapper mapper = new ObjectMapper();
    var result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(yml);

    JsonNode actualObj = mapper.readTree(result);

    String certificateAuthorityData = actualObj.get("clusters").get(0).get("cluster").get(
        "certificate-authority-data")
        .toString().replace("\"", "");

    byte[] byteCertificateAuthData = Base64.getDecoder().decode(certificateAuthorityData.getBytes("UTF-8"));

    System.out.println("Token: " + jwt.getRawToken());
    ApiClient client = new ClientBuilder().setBasePath(url)
        .setAuthentication(new AccessTokenAuthentication(jwt.getRawToken()))
        .setVerifyingSsl(false)
        .setCertificateAuthority(byteCertificateAuthData).build();
    CoreV1Api coreApi = new CoreV1Api(client);

    System.out.println("ExternalIP: " + "https://" + masterIP + ":" + nodePort);

    System.out.println("hehey: " + coreApi.listNamespace(null, null, null, null, null, null, null, null, null, null));
    return null;
  }

}
