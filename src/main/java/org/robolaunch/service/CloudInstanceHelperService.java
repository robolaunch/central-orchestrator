package org.robolaunch.service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
import org.robolaunch.models.Result;
import org.robolaunch.models.CreateRCResult;
import org.robolaunch.repository.abstracts.CloudInstanceHelperRepository;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.options.ListOptions;
import io.minio.errors.MinioException;
import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class CloudInstanceHelperService {
  @Inject
  Logger log;

  @Inject
  KogitoService kogitoService;

  @Inject
  KubernetesService kubernetesService;

  @Inject
  CloudInstanceHelperRepository cloudInstanceHelperRepository;

  @Inject
  JsonWebToken jwt;

  @LoggerName("cloudInstanceHelperService")
  Logger cloudInstanceHelperLogger;

  @Inject
  ApiClientManager apiClientManager;

  public String stageSetter(String newStage) {
    try {
      String newestStage = newStage;
      return newestStage;
    } catch (Exception e) {
      return null;
    }
  }

  public Result limitResult(CreateRCResult tCreateRCResult) {
    return new Result(tCreateRCResult.getReason(), false);
  }

  public Result errorNotAuthorizedResult() {
    return new Result("You are not authorized.", false);
  }

  public Result alreadyExistsResult() {
    return new Result("A Cloud Instance with this name already exists.", false);
  }

  public Result mustBeStoppedResult() {
    return new Result("You must stop the cloud instance before termination.", false);
  }

  public Result errorCreatingCloudInstance() {
    return new Result("Error creating cloud instance", false);
  }

  public Result errorStartingCloudInstance() {
    return new Result("Error starting cloud instance", false);
  }

  public String getBufferName(Organization organization, String teamId, String cloudInstanceName, String region)
      throws IOException, ApiException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, MinioException {
    ApiClient apiClient = cloudInstanceHelperRepository.adminApiClient("eu-central-1");

    DynamicKubernetesApi vcApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
        "virtualclusters", apiClient);
    ListOptions listOptions = new ListOptions();
    listOptions.setLabelSelector("robolaunch.io/organization=" + organization.getName() + ",robolaunch.io/team="
        + teamId + ",robolaunch.io/cloud-instance=" + cloudInstanceName);

    return vcApi.list(listOptions).getObject().getItems().get(0).getMetadata().getName()
        .split("-")[1];
  }

  public void bufferCall(String instanceType, String region) {
    try {
      cloudInstanceHelperRepository.bufferCall(instanceType, region);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Buffer call failed");
    }
  }

  public void CIOperationCall(String processId, String operation, String region) {
    try {
      cloudInstanceHelperRepository.CIOperationCall(processId, operation, region);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling CIOperationCall: " + e.getMessage());
    }
  }

  public String getProcessId(Organization organization, String teamId) {
    try {
      return kogitoService.getProcessId(organization, teamId);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting process id from kubernetes buffer", e);
      return null;
    }
  }

  public Boolean isMachineCreated(String bufferName, String region) {
    try {
      return cloudInstanceHelperRepository.isMachineCreated(bufferName, region);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error creating cloud instance");
      return false;
    }
  }

  public String generateBufferName() {
    try {
      return cloudInstanceHelperRepository.generateBufferName();
    } catch (Exception e) {
      return null;
    }
  }

  public ApiClient getVirtualClusterClientWithBufferName(String bufferName, String region) {
    try {
      return cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName, region);
    } catch (Exception e) {
      return null;
    }
  }

  public String getGeneratedMachineName(String bufferName, String region) {
    try {
      String machineName = cloudInstanceHelperRepository.getGeneratedMachineName(bufferName, region);
      return machineName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error getting machne name", e);
      return null;
    }
  }

  public Boolean nodeRefChecker(String bufferName, String machineName, String region) {
    try {
      Boolean isReady = cloudInstanceHelperRepository.nodeRefChecker(bufferName, machineName, region);
      return isReady;
    } catch (Exception e) {
      return null;
    }
  }

  public Boolean isVirtualClusterReady(String bufferName, String region) {
    try {
      Boolean isReady = cloudInstanceHelperRepository.isVirtualClusterReady(bufferName, region);
      return isReady;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while creating virtual cluster.", e);
      return null;
    }
  }

  public Integer getBufferingVirtualClusterCount(String instanceType, String region) {
    try {
      if (cloudInstanceHelperRepository.getBufferingVirtualClusterCount(instanceType, region) != null) {
        Integer count = cloudInstanceHelperRepository.getBufferingVirtualClusterCount(instanceType, region);
        cloudInstanceHelperLogger.info("Buffering -" + instanceType + "- virtual cluster count: " + count);
        return count;
      } else {
        return 0;
      }
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting buffering virtual cluster count.", e);
      return null;
    }
  }

  public Integer getBufferedVirtualClusterCount(String instanceType, String region) {
    try {
      Integer count = cloudInstanceHelperRepository.getBufferedVirtualClusterCount(instanceType, region);
      System.out.println("Buffered count: " + count);
      cloudInstanceHelperLogger.info("Buffered -" + instanceType + "- virtual cluster count: " + count);
      return count;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting buffered virtual cluster count.", e);
      return null;
    }
  }

  public String selectBufferedVirtualCluster(Integer vcCount, String region) {
    try {
      String bufferName = cloudInstanceHelperRepository.selectBufferedVirtualCluster(vcCount, region);
      cloudInstanceHelperLogger.info("Buffered virtual cluster selected");
      return bufferName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while selecting buffered virtual cluster.", e);
      return null;
    }
  }

  public String selectNode(String bufferName, String region) {
    try {
      String nodeName = cloudInstanceHelperRepository.selectNode(bufferName, region);
      cloudInstanceHelperLogger.info("Node name: " + nodeName);
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting node name.", e);
      return null;
    }
  }

  public String getNodeName(String machineName, String region) {
    try {
      String nodeName = cloudInstanceHelperRepository.getNodeName(machineName, region);
      cloudInstanceHelperLogger.info("Node name: " + nodeName);
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting node name.", e);
      return null;
    }
  }

  public String getGeneratedNodeName(String bufferName, String machineName, String region) {
    try {
      String nodeName = cloudInstanceHelperRepository.getNodeName(machineName, region);
      cloudInstanceHelperLogger.info("Node name: " + nodeName);
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting node name.", e);
      return null;
    }
  }

  public String getNamespaceName(String bufferName, String region) {
    try {
      String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName, region);
      cloudInstanceHelperLogger.info("Namespace name: " + namespaceName);
      return namespaceName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting namespace name.", e);
      return null;
    }
  }

  public Boolean isCoreDNSDeploymentUp(String bufferName, String region) {
    try {
      Boolean isCoreDNSDeploymentUp = cloudInstanceHelperRepository.isCoreDNSDeploymentUp(bufferName, region);
      return isCoreDNSDeploymentUp;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking CoreDNS deployment status.", e);
      return false;
    }
  }

  public Boolean isCertManagerReady(String bufferName, String region) {
    try {
      return cloudInstanceHelperRepository.isCertManagerReady(bufferName, region);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking if cert manager is ready.", e);
      return false;
    }
  }

  public Boolean isStatefulSetsUp(String namespaceName, String region) {
    try {
      Boolean isResourcesUp = cloudInstanceHelperRepository.isStatefulSetsUp(namespaceName, region);
      return isResourcesUp;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking resources up.", e);
      return false;
    }
  }

  public Boolean isStatefulSetsDown(String namespaceName, String region) {
    try {
      Boolean isResourcesDown = cloudInstanceHelperRepository.isStatefulSetsDown(namespaceName, region);
      return isResourcesDown;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking resources up.", e);
      return false;
    }
  }

  public Response deleteDNSRecord(Organization organization, String nodeName, String region) {
    try {
      cloudInstanceHelperRepository.deleteDNSRecord(organization, nodeName, region);
      cloudInstanceHelperLogger.info("DNS Record deleted");
      return new Response(true, "DNS Record deleted.");
    } catch (Exception e) {
      return new Response(false, "Error deleting DNS Record.");
    }
  }

  public Boolean isNodeUnschedulable(String nodeName, String region) {
    try {
      Boolean isNodeUnschedulable = cloudInstanceHelperRepository.isNodeUnschedulable(nodeName, region);
      cloudInstanceHelperLogger.info("Node unschedulable checked");
      return isNodeUnschedulable;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking node unschedulable.", e);
      return null;
    }
  }

  public Boolean isNodeReady(String nodeName, String region) {
    try {
      Boolean isNodeReady = cloudInstanceHelperRepository.isNodeReady(nodeName, region);
      return isNodeReady;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking node ready.", e);
      return null;
    }
  }

  public Response deleteClusterVersion(String bufferName, String region) {
    try {
      cloudInstanceHelperRepository.deleteClusterVersion(bufferName, region);
      cloudInstanceHelperLogger.info("Cluster version deleted");
      return new Response(true, "Cluster version deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting cluster version.", e);
      return new Response(false, "Error deleting cluster version.");
    }
  }

  public Response deleteOAuth2ProxyResources(String bufferName, String region) {
    try {
      cloudInstanceHelperRepository.deleteOAuth2ProxyResources(bufferName, region);
      cloudInstanceHelperLogger.info("OAuth2 Proxy resources deleted");
      return new Response(true, "OAuth2 Proxy resources deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting OAuth2 Proxy resources.", e);
      return new Response(false, "Error deleting OAuth2 Proxy resources.");
    }
  }

  public Response deleteVirtualCluster(String bufferName, String region) {
    try {
      cloudInstanceHelperRepository.deleteVirtualCluster(bufferName, region);
      cloudInstanceHelperLogger.info("Virtual cluster deleted");
      return new Response(true, "Virtual cluster deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting virtual cluster.", e);
      return new Response(false, "Error deleting virtual cluster.");
    }
  }

  public Boolean isSubnetUsed(String bufferName, String region) {
    try {
      Boolean isSubnetUsed = cloudInstanceHelperRepository.isSubnetUsed(bufferName, region);
      cloudInstanceHelperLogger.info("Subnet used checked");
      return isSubnetUsed;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking subnet used.", e);
      return null;
    }
  }

  public String getCloudInstanceIP(String bufferName, String region) {
    try {
      String cloudInstanceIP = cloudInstanceHelperRepository.getCloudInstanceIP(bufferName, region);
      cloudInstanceHelperLogger.info("Cloud instance IP: " + cloudInstanceIP);
      return cloudInstanceIP;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting cloud instance IP.", e);
      return null;
    }
  }

  public Response deleteSubnet(String bufferName, String region) {
    try {
      cloudInstanceHelperRepository.deleteSubnet(bufferName, region);
      cloudInstanceHelperLogger.info("Subnet deleted");
      return new Response(true, "Subnet deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting subnet.", e);
      return new Response(false, "Error deleting subnet.");
    }
  }

  public Response deleteMachineDeployment(String bufferName, String region) {
    try {
      cloudInstanceHelperRepository.deleteMachineDeployment(bufferName, region);
      cloudInstanceHelperLogger.info("Machine deployment deleted");
      return new Response(true, "Machine deployment deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting machine deployment.", e);
      return new Response(false, "Error deleting machine deployment.");
    }
  }

  public Response deleteOrganizationLabelsFromNode(String nodeName, String region) {
    try {
      cloudInstanceHelperRepository.deleteOrganizationLabelsFromSuperCluster(nodeName, region);
      cloudInstanceHelperLogger.info("Node unlabelled");
      return new Response(true, "Node unlabelled.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while unlabelling node.", e);
      return new Response(false, "Error unlabelling node.");
    }
  }

  public Response deleteWorkerLabelFromNode(String nodeName, String region) {
    try {
      cloudInstanceHelperRepository.deleteWorkerLabelFromNode(nodeName, region);
      cloudInstanceHelperLogger.info("Node unlabelled");
      return new Response(true, "Node unlabelled.");
    } catch (Exception e) {
      return new Response(false, "Error unlabelling node.");
    }
  }

  public String findNode(String bufferName, Organization organization, String teamId,
      String cloudInstanceName, String region) {
    try {
      String nodeName = cloudInstanceHelperRepository.findNode(bufferName, organization, teamId,
          cloudInstanceName, region);
      cloudInstanceHelperLogger.info("Node found");
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while finding node.", e);
      return null;
    }
  }

  public Response deleteVCNodes(String bufferName, String region) {
    try {
      cloudInstanceHelperRepository.deleteVirtualClusterNodes(bufferName, region);
      cloudInstanceHelperLogger.info("VC nodes deleted");
      return new Response(true, "VC nodes deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting VC nodes.", e);
      return new Response(false, "Error deleting VC nodes.");
    }
  }

  public Boolean healthCheck(Organization organization, String teamId, String cloudInstanceName,
      String nodeName, String region) {
    try {
      Boolean healthCheck = cloudInstanceHelperRepository.healthCheck(organization, teamId, cloudInstanceName,
          nodeName, region);
      return healthCheck;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while health check.", e);
      return false;
    }
  }

  public String convertJsonStringToYamlString(String jsonString) {
    try {
      return cloudInstanceHelperRepository.convertJsonStringToYamlString(jsonString);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling convertJsonStringToYamlString: " + e.getMessage());
      return null;
    }
  }

  public String getNamespaceNameWithBufferName(String bufferName, String region) {
    try {
      return cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName, region);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling getNamespaceNameWithBufferName: " + e.getMessage());
      return null;
    }
  }

  public String getTeamIdFromProcessId(String processId, String region) {
    try {
      String teamId = cloudInstanceHelperRepository.getTeamIdFromProcessId(processId, region);
      return teamId;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling getTeamIdFromProcessId: " + e.getMessage());
      return null;
    }
  }

  public Boolean doesCloudInstanceExist(Organization organization, String teamId, String cloudInstanceName,
      String region) {
    try {
      Boolean doesCloudInstanceExist = cloudInstanceHelperRepository.doesCloudInstanceExist(organization, teamId,
          cloudInstanceName, region);
      return doesCloudInstanceExist;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling doesCloudInstanceExist: " + e.getMessage());
      return null;
    }
  }

  public void connectAdminClient(String region) {
    try {
      ApiClient myApiCl = apiClientManager.getAdminApiClient(region);
      System.out.println("GOT THE CLIENT HERE!");
      CoreV1Api api = new CoreV1Api(myApiCl);
      for (V1Namespace ns : api.listNamespace(null, null, null, null, null, null, null, null, null, null).getItems()) {
        System.out.println("MY NSSS: " + ns.getMetadata().getName());
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
