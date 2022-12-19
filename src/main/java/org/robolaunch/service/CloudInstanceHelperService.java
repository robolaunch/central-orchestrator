package org.robolaunch.service;

import java.io.IOException;

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
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.options.ListOptions;
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

  public String getBufferName(Organization organization, String departmentName, String cloudInstanceName)
      throws IOException, ApiException, InterruptedException {
    ApiClient apiClient = cloudInstanceHelperRepository.adminApiClient();

    DynamicKubernetesApi vcApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
        "virtualclusters", apiClient);
    ListOptions listOptions = new ListOptions();
    listOptions.setLabelSelector("robolaunch.io/organization=" + organization.getName() + ",robolaunch.io/team="
        + departmentName + ",robolaunch.io/cloud-instance=" + cloudInstanceName);

    return vcApi.list("default", listOptions).getObject().getItems().get(0).getMetadata().getName()
        .split("-")[1];
  }

  public void bufferCall(String instanceType) {
    try {
      System.out.println("Buffer call: " + instanceType);
      cloudInstanceHelperRepository.bufferCall(instanceType);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Buffer call failed");
    }
  }

  public void CIOperationCall(String processId, String operation) {
    try {
      cloudInstanceHelperRepository.CIOperationCall(processId, operation);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling CIOperationCall: " + e.getMessage());
    }
  }

  public String getProcessId(Organization organization, String departmentName) {
    try {
      return kogitoService.getProcessId(organization, departmentName);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting process id from kubernetes buffer", e);
      return null;
    }
  }

  public Boolean isMachineCreated(String bufferName) {
    try {
      return cloudInstanceHelperRepository.isMachineCreated(bufferName);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error creating cloud instance");
      return false;
    }
  }

  public String generateBufferName() {
    try {
      return cloudInstanceHelperRepository.generateBufferName();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.out.println(e.getCause());
      return null;
    }
  }

  public ApiClient getVirtualClusterClientWithBufferName(String bufferName) {
    try {
      return cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.out.println(e.getCause());
      return null;
    }
  }

  public String getGeneratedMachineName(String bufferName) {
    try {
      String machineName = cloudInstanceHelperRepository.getGeneratedMachineName(bufferName);
      return machineName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error getting machne name", e);
      return null;
    }
  }

  public Boolean nodeRefChecker(String bufferName, String machineName) {
    try {
      Boolean isReady = cloudInstanceHelperRepository.nodeRefChecker(bufferName, machineName);
      System.out.println("Is ready: " + isReady);
      return isReady;
    } catch (Exception e) {
      return null;
    }
  }

  public Boolean isVirtualClusterReady(String bufferName) {
    try {
      Boolean isReady = cloudInstanceHelperRepository.isVirtualClusterReady(bufferName);
      cloudInstanceHelperLogger.info("Is vc ready: " + isReady);
      return isReady;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while creating virtual cluster.", e);
      return null;
    }
  }

  public Integer getBufferingVirtualClusterCount(String instanceType) {
    try {
      if (cloudInstanceHelperRepository.getBufferingVirtualClusterCount(instanceType) != null) {
        Integer count = cloudInstanceHelperRepository.getBufferingVirtualClusterCount(instanceType);
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

  public Integer getBufferedVirtualClusterCount(String instanceType) {
    try {
      Integer count = cloudInstanceHelperRepository.getBufferedVirtualClusterCount(instanceType);
      cloudInstanceHelperLogger.info("Buffered -" + instanceType + "- virtual cluster count: " + count);
      return count;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting buffered virtual cluster count.", e);
      return null;
    }
  }

  public String selectBufferedVirtualCluster(Integer vcCount) {
    try {
      String bufferName = cloudInstanceHelperRepository.selectBufferedVirtualCluster(vcCount);
      cloudInstanceHelperLogger.info("Buffered virtual cluster selected");
      return bufferName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while selecting buffered virtual cluster.", e);
      return null;
    }
  }

  public String selectNode(String bufferName) {
    try {
      String nodeName = cloudInstanceHelperRepository.selectNode(bufferName);
      cloudInstanceHelperLogger.info("Node name: " + nodeName);
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting node name.", e);
      return null;
    }
  }

  public String getNodeName(String machineName) {
    try {
      String nodeName = cloudInstanceHelperRepository.getNodeName(machineName);
      cloudInstanceHelperLogger.info("Node name: " + nodeName);
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting node name.", e);
      return null;
    }
  }

  public String getGeneratedNodeName(String bufferName, String machineName) {
    try {
      String nodeName = cloudInstanceHelperRepository.getNodeName(machineName);
      cloudInstanceHelperLogger.info("Node name: " + nodeName);
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting node name.", e);
      return null;
    }
  }

  public String getNamespaceName(String bufferName) {
    try {
      String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName);
      cloudInstanceHelperLogger.info("Namespace name: " + namespaceName);
      return namespaceName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting namespace name.", e);
      return null;
    }
  }

  public Boolean isCoreDNSDeploymentUp(String bufferName) {
    try {
      Boolean isCoreDNSDeploymentUp = cloudInstanceHelperRepository.isCoreDNSDeploymentUp(bufferName);
      cloudInstanceHelperLogger.info("CoreDNS deployment status: " + isCoreDNSDeploymentUp);
      return isCoreDNSDeploymentUp;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking CoreDNS deployment status.", e);
      return false;
    }
  }

  public Boolean isCertManagerReady(String namespaceName) {
    try {
      return cloudInstanceHelperRepository.isCertManagerReady(namespaceName);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking if cert manager is ready.", e);
      return false;
    }
  }

  public Boolean isStatefulSetsUp(String namespaceName) {
    try {
      Boolean isResourcesUp = cloudInstanceHelperRepository.isStatefulSetsUp(namespaceName);
      System.out.println("is resources up?: " + isResourcesUp);
      return isResourcesUp;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking resources up.", e);
      return false;
    }
  }

  public Boolean isStatefulSetsDown(String namespaceName) {
    try {
      Boolean isResourcesDown = cloudInstanceHelperRepository.isStatefulSetsDown(namespaceName);
      System.out.println("is resources down?: " + isResourcesDown);
      return isResourcesDown;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking resources up.", e);
      return false;
    }
  }

  public Response deleteDNSRecord(Organization organization, String nodeName) {
    try {
      cloudInstanceHelperRepository.deleteDNSRecord(organization, nodeName);
      cloudInstanceHelperLogger.info("DNS Record deleted");
      return new Response(true, "DNS Record deleted.");
    } catch (Exception e) {
      return new Response(false, "Error deleting DNS Record.");
    }
  }

  public Boolean isNodeUnschedulable(String nodeName) {
    try {
      Boolean isNodeUnschedulable = cloudInstanceHelperRepository.isNodeUnschedulable(nodeName);
      cloudInstanceHelperLogger.info("Node unschedulable checked");
      return isNodeUnschedulable;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking node unschedulable.", e);
      return null;
    }
  }

  public Boolean isNodeReady(String nodeName) {
    try {
      Boolean isNodeReady = cloudInstanceHelperRepository.isNodeReady(nodeName);
      cloudInstanceHelperLogger.info("Node ready checked");
      return isNodeReady;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking node ready.", e);
      return null;
    }
  }

  public Response deleteClusterVersion(String bufferName) {
    try {
      cloudInstanceHelperRepository.deleteClusterVersion(bufferName);
      cloudInstanceHelperLogger.info("Cluster version deleted");
      return new Response(true, "Cluster version deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting cluster version.", e);
      return new Response(false, "Error deleting cluster version.");
    }
  }

  public Response deleteOAuth2ProxyResources(String bufferName) {
    try {
      cloudInstanceHelperRepository.deleteOAuth2ProxyResources(bufferName);
      cloudInstanceHelperLogger.info("OAuth2 Proxy resources deleted");
      return new Response(true, "OAuth2 Proxy resources deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting OAuth2 Proxy resources.", e);
      return new Response(false, "Error deleting OAuth2 Proxy resources.");
    }
  }

  public Response deleteVirtualCluster(String bufferName) {
    try {
      cloudInstanceHelperRepository.deleteVirtualCluster(bufferName);
      cloudInstanceHelperLogger.info("Virtual cluster deleted");
      return new Response(true, "Virtual cluster deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting virtual cluster.", e);
      return new Response(false, "Error deleting virtual cluster.");
    }
  }

  public Boolean isSubnetUsed(String bufferName) {
    try {
      Boolean isSubnetUsed = cloudInstanceHelperRepository.isSubnetUsed(bufferName);
      cloudInstanceHelperLogger.info("Subnet used checked");
      System.out.println("isSubnetUsed: " + isSubnetUsed);
      return isSubnetUsed;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking subnet used.", e);
      return null;
    }
  }

  public String getCloudInstanceIP(String bufferName) {
    try {
      String cloudInstanceIP = cloudInstanceHelperRepository.getCloudInstanceIP(bufferName);
      cloudInstanceHelperLogger.info("Cloud instance IP: " + cloudInstanceIP);
      return cloudInstanceIP;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting cloud instance IP.", e);
      return null;
    }
  }

  public Response deleteSubnet(String bufferName) {
    try {
      cloudInstanceHelperRepository.deleteSubnet(bufferName);
      cloudInstanceHelperLogger.info("Subnet deleted");
      return new Response(true, "Subnet deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting subnet.", e);
      return new Response(false, "Error deleting subnet.");
    }
  }

  public Response deleteMachineDeployment(String bufferName) {
    try {
      cloudInstanceHelperRepository.deleteMachineDeployment(bufferName);
      cloudInstanceHelperLogger.info("Machine deployment deleted");
      return new Response(true, "Machine deployment deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting machine deployment.", e);
      return new Response(false, "Error deleting machine deployment.");
    }
  }

  public Response deleteOrganizationLabelsFromNode(String nodeName) {
    try {
      cloudInstanceHelperRepository.deleteOrganizationLabelsFromSuperCluster(nodeName);
      cloudInstanceHelperLogger.info("Node unlabelled");
      return new Response(true, "Node unlabelled.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while unlabelling node.", e);
      return new Response(false, "Error unlabelling node.");
    }
  }

  public Response deleteWorkerLabelFromNode(String nodeName) {
    try {
      cloudInstanceHelperRepository.deleteWorkerLabelFromNode(nodeName);
      cloudInstanceHelperLogger.info("Node unlabelled");
      return new Response(true, "Node unlabelled.");
    } catch (Exception e) {
      return new Response(false, "Error unlabelling node.");
    }
  }

  public String findNode(String bufferName, Organization organization, String departmentName,
      String cloudInstanceName) {
    try {
      String nodeName = cloudInstanceHelperRepository.findNode(bufferName, organization, departmentName,
          cloudInstanceName);
      cloudInstanceHelperLogger.info("Node found");
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while finding node.", e);
      return null;
    }
  }

  public Response deleteVCNodes(String bufferName) {
    try {
      cloudInstanceHelperRepository.deleteVirtualClusterNodes(bufferName);
      cloudInstanceHelperLogger.info("VC nodes deleted");
      return new Response(true, "VC nodes deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting VC nodes.", e);
      return new Response(false, "Error deleting VC nodes.");
    }
  }

  public Boolean healthCheck(Organization organization, String departmentName, String cloudInstanceName,
      String nodeName) {
    try {
      Boolean healthCheck = cloudInstanceHelperRepository.healthCheck(organization, departmentName, cloudInstanceName,
          nodeName);
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

  public String getNamespaceNameWithBufferName(String bufferName) {
    try {
      return cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling getNamespaceNameWithBufferName: " + e.getMessage());
      return null;
    }
  }

  public String getTeamIdFromProcessId(String processId) {
    try {
      System.out.println("processId: " + processId);
      String teamId = cloudInstanceHelperRepository.getTeamIdFromProcessId(processId);
      System.out.println("teamId: " + teamId);
      return teamId;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling getTeamIdFromProcessId: " + e.getMessage());
      return null;
    }
  }

  public Boolean doesCloudInstanceExist(Organization organization, String teamId, String cloudInstanceName) {
    try {
      Boolean doesCloudInstanceExist = cloudInstanceHelperRepository.doesCloudInstanceExist(organization, teamId,
          cloudInstanceName);
      return doesCloudInstanceExist;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling doesCloudInstanceExist: " + e.getMessage());
      return null;
    }
  }

  public void createCRB(String bufferName) {
    try {
      cloudInstanceHelperRepository.createCRB(bufferName);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling createCRB: " + e.getMessage());
    }
  }

  public void userApiClient(String bufferName) throws IOException, InterruptedException {
    try {
      String token = jwt.getRawToken();
      cloudInstanceHelperRepository.testingUserApiClient(bufferName, token);
      cloudInstanceHelperLogger.info("User ApiClient tested!");
    } catch (ApiException e) {
      cloudInstanceHelperLogger.error("Error while testing user ApiClient: " + e.getMessage());
      System.out.println(e.getResponseBody());
      System.out.println(e.getCode());
    }
  }

  public void adminApiClient() {
    try {
      cloudInstanceHelperRepository.testingAdminApiClient();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

}
