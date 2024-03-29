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

import com.google.gson.Gson;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
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

  public String getBufferName(Organization organization, String teamId, String cloudInstanceName, String provider,
      String region, String superCluster)
      throws IOException, ApiException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException,
      IllegalArgumentException, MinioException {
    ApiClient apiClient = apiClientManager.getAdminApiClient(provider, region, superCluster);

    DynamicKubernetesApi vcApi = new DynamicKubernetesApi("tenancy.x-k8s.io", "v1alpha1",
        "virtualclusters", apiClient);
    ListOptions listOptions = new ListOptions();
    listOptions.setLabelSelector("robolaunch.io/organization=" + organization.getName() + ",robolaunch.io/team="
        + teamId + ",robolaunch.io/cloud-instance=" + cloudInstanceName);
    Gson gson = new Gson();
    System.out.println("gsge: " + gson.toJson(vcApi.list(listOptions).getObject().getItems()));
    if (vcApi.list(listOptions).getObject().getItems().size() == 0) {
      cloudInstanceHelperLogger.info("No virtual cluster found for this cloud instance");
      return null;
    }
    String vcName = vcApi.list(listOptions).getObject().getItems().get(0).getMetadata().getName();
    return vcName;
  }

  public String getProcessId(Organization organization, String teamId) {
    try {
      return kogitoService.getProcessId(organization, teamId);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting process id from kubernetes buffer", e);
      return null;
    }
  }

  public Boolean isMachineCreated(String bufferName, String provider, String region, String superCluster) {
    try {
      return cloudInstanceHelperRepository.isMachineCreated(bufferName, provider, region, superCluster);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error creating cloud instance");
      return false;
    }
  }

  public Boolean isConnectionHubOperatorReady(String bufferName, String provider, String region, String superCluster,
      String namespaceName) {
    try {
      Boolean isReady = cloudInstanceHelperRepository.isConnectionHubOperatorReady(bufferName, provider, region,
          superCluster,
          namespaceName);
      cloudInstanceHelperLogger.info("Connection hub operator is ready: " + isReady);
      return isReady;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error checking connection hub operator", e);
      return false;
    }
  }

  public String generateBufferName() {
    try {
      String generatedBufferName = cloudInstanceHelperRepository.generateBufferName();
      cloudInstanceHelperLogger.info("Buffer name generated: " + generatedBufferName);
      return generatedBufferName;
    } catch (Exception e) {
      return null;
    }
  }

  public ApiClient getVirtualClusterClientWithBufferName(String bufferName, String provider, String region,
      String superCluster) {
    try {
      return cloudInstanceHelperRepository.getVirtualClusterClientWithBufferName(bufferName, provider, region,
          superCluster);
    } catch (Exception e) {
      return null;
    }
  }

  public String getGeneratedMachineName(String bufferName, String provider, String region, String superCluster) {
    try {
      String machineName = cloudInstanceHelperRepository.getGeneratedMachineName(bufferName, provider, region,
          superCluster);
      return machineName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error getting machne name", e);
      return null;
    }
  }

  public Boolean nodeRefChecker(String bufferName, String machineName, String provider, String region,
      String superCluster) {
    try {
      Boolean isReady = cloudInstanceHelperRepository.nodeRefChecker(bufferName, machineName, provider, region,
          superCluster);
      return isReady;
    } catch (Exception e) {
      return null;
    }
  }

  public Boolean isVirtualClusterReady(String bufferName, String provider, String region, String superCluster) {
    try {
      Boolean isReady = cloudInstanceHelperRepository.isVirtualClusterReady(bufferName, provider, region, superCluster);
      return isReady;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking if virtual cluster ready.", e);
      return false;
    }
  }

  public String selectBufferedVirtualCluster(Integer vcCount, String provider, String region, String superCluster) {
    try {
      String bufferName = cloudInstanceHelperRepository.selectBufferedVirtualCluster(vcCount, provider, region,
          superCluster);
      cloudInstanceHelperLogger.info("Buffered virtual cluster selected");
      return bufferName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while selecting buffered virtual cluster.", e);
      return null;
    }
  }

  public String selectNode(String bufferName, String provider, String region, String superCluster) {
    try {
      String nodeName = cloudInstanceHelperRepository.selectNode(bufferName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Node name: " + nodeName);
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting node name.", e);
      return null;
    }
  }

  public String getNodeName(String machineName, String provider, String region, String superCluster) {
    try {
      String nodeName = cloudInstanceHelperRepository.getNodeName(machineName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Node name: " + nodeName);
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting node name.", e);
      return null;
    }
  }

  public String getGeneratedNodeName(String bufferName, String machineName, String provider, String region,
      String superCluster) {
    try {
      String nodeName = cloudInstanceHelperRepository.getNodeName(machineName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Node name: " + nodeName);
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting node name.", e);
      return null;
    }
  }

  public String getNamespaceName(String bufferName, String provider, String region, String superCluster) {
    try {
      String namespaceName = cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName, provider, region,
          superCluster);
      cloudInstanceHelperLogger.info("Namespace name: " + namespaceName);
      return namespaceName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting namespace name.", e);
      return null;
    }
  }

  public Boolean isCoreDNSDeploymentUp(String bufferName, String provider, String region, String superCluster) {
    try {
      Boolean isCoreDNSDeploymentUp = cloudInstanceHelperRepository.isCoreDNSDeploymentUp(bufferName, provider, region,
          superCluster);
      return isCoreDNSDeploymentUp;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking CoreDNS deployment status.", e);
      return false;
    }
  }

  public Boolean isCertManagerReady(String bufferName, String provider, String region, String superCluster) {
    try {
      return cloudInstanceHelperRepository.isCertManagerReady(bufferName, provider, region, superCluster);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking if cert manager is ready.", e);
      return false;
    }
  }

  public Boolean isStatefulSetsUp(String namespaceName, String provider, String region, String superCluster) {
    try {
      Boolean isResourcesUp = cloudInstanceHelperRepository.isStatefulSetsUp(namespaceName, provider, region,
          superCluster);
      return isResourcesUp;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking resources up.", e);
      return false;
    }
  }

  public Boolean isStatefulSetsDown(String namespaceName, String provider, String region, String superCluster) {
    try {
      Boolean isResourcesDown = cloudInstanceHelperRepository.isStatefulSetsDown(namespaceName, provider, region,
          superCluster);
      return isResourcesDown;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking resources up.", e);
      return false;
    }
  }

  public Response deleteDNSRecord(Organization organization, String nodeName, String provider, String region,
      String superCluster) {
    try {
      cloudInstanceHelperRepository.deleteDNSRecord(organization, nodeName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("DNS Record deleted");
      return new Response(true, "DNS Record deleted.");
    } catch (Exception e) {
      return new Response(false, "Error deleting DNS Record.");
    }
  }

  public Boolean isNodeUnschedulable(String nodeName, String provider, String region, String superCluster) {
    try {
      Boolean isNodeUnschedulable = cloudInstanceHelperRepository.isNodeUnschedulable(nodeName, provider, region,
          superCluster);
      cloudInstanceHelperLogger.info("Node unschedulable checked");
      return isNodeUnschedulable;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking node unschedulable.", e);
      return null;
    }
  }

  public Boolean isNodeReady(String nodeName, String provider, String region, String superCluster) {
    try {
      Boolean isNodeReady = cloudInstanceHelperRepository.isNodeReady(nodeName, provider, region, superCluster);
      return isNodeReady;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking node ready.", e);
      return null;
    }
  }

  public Response deleteClusterVersion(String bufferName, String provider, String region, String superCluster) {
    try {
      cloudInstanceHelperRepository.deleteClusterVersion(bufferName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Cluster version deleted");
      return new Response(true, "Cluster version deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting cluster version.", e);
      return new Response(false, "Error deleting cluster version.");
    }
  }

  public Response deleteOAuth2ProxyResources(String bufferName, String provider, String region, String superCluster) {
    try {
      cloudInstanceHelperRepository.deleteOAuth2ProxyResources(bufferName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("OAuth2 Proxy resources deleted");
      return new Response(true, "OAuth2 Proxy resources deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting OAuth2 Proxy resources.", e);
      return new Response(false, "Error deleting OAuth2 Proxy resources.");
    }
  }

  public Response deleteVirtualCluster(String bufferName, String provider, String region, String superCluster) {
    try {
      cloudInstanceHelperRepository.deleteVirtualCluster(bufferName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Virtual cluster deleted");
      return new Response(true, "Virtual cluster deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting virtual cluster.", e);
      return new Response(false, "Error deleting virtual cluster.");
    }
  }

  public Boolean isSubnetUsed(String bufferName, String provider, String region, String superCluster) {
    try {
      Boolean isSubnetUsed = cloudInstanceHelperRepository.isSubnetUsed(bufferName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Subnet used checked");
      return isSubnetUsed;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while checking subnet used.", e);
      return null;
    }
  }

  public String getCloudInstanceIP(String bufferName, String provider, String region, String superCluster) {
    try {
      String cloudInstanceIP = cloudInstanceHelperRepository.getCloudInstanceIP(bufferName, provider, region,
          superCluster);
      cloudInstanceHelperLogger.info("Cloud instance IP: " + cloudInstanceIP);
      return cloudInstanceIP;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while getting cloud instance IP.", e);
      return null;
    }
  }

  public Response deleteSubnet(String bufferName, String provider, String region, String superCluster) {
    try {
      cloudInstanceHelperRepository.deleteSubnet(bufferName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Subnet deleted");
      return new Response(true, "Subnet deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting subnet.", e);
      return new Response(false, "Error deleting subnet.");
    }
  }

  public Response deleteMachineDeployment(String bufferName, String provider, String region, String superCluster) {
    try {
      cloudInstanceHelperRepository.deleteMachineDeployment(bufferName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Machine deployment deleted");
      return new Response(true, "Machine deployment deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting machine deployment.", e);
      return new Response(false, "Error deleting machine deployment.");
    }
  }

  public Response deleteOrganizationLabelsFromNode(String nodeName, String provider, String region,
      String superCluster) {
    try {
      cloudInstanceHelperRepository.deleteOrganizationLabelsFromSuperCluster(nodeName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Node unlabelled");
      return new Response(true, "Node unlabelled.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while unlabelling node.", e);
      return new Response(false, "Error unlabelling node.");
    }
  }

  public Response deleteWorkerLabelFromNode(String nodeName, String provider, String region, String superCluster) {
    try {
      cloudInstanceHelperRepository.deleteWorkerLabelFromNode(nodeName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Node unlabelled");
      return new Response(true, "Node unlabelled.");
    } catch (Exception e) {
      return new Response(false, "Error unlabelling node.");
    }
  }

  public String findNode(String bufferName, Organization organization, String teamId,
      String cloudInstanceName, String provider, String region, String superCluster) {
    try {
      String nodeName = cloudInstanceHelperRepository.findNode(bufferName, organization, teamId,
          cloudInstanceName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("Node found");
      return nodeName;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while finding node.", e);
      return null;
    }
  }

  public Response deleteVCNodes(String bufferName, String provider, String region, String superCluster) {
    try {
      cloudInstanceHelperRepository.deleteVirtualClusterNodes(bufferName, provider, region, superCluster);
      cloudInstanceHelperLogger.info("VC nodes deleted");
      return new Response(true, "VC nodes deleted.");
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while deleting VC nodes.", e);
      return new Response(false, "Error deleting VC nodes.");
    }
  }

  public Boolean healthCheck(Organization organization, String teamId, String cloudInstanceName,
      String nodeName, String provider, String region, String superCluster) {
    try {
      Boolean healthCheck = cloudInstanceHelperRepository.healthCheck(organization, teamId, cloudInstanceName,
          nodeName, provider, region, superCluster);
      // CORRECT IT
      return true;
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

  public String getNamespaceNameWithBufferName(String bufferName, String provider, String region, String superCluster) {
    try {
      return cloudInstanceHelperRepository.getNamespaceNameWithBufferName(bufferName, provider, region, superCluster);
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling getNamespaceNameWithBufferName: " + e.getMessage());
      return null;
    }
  }

  public String getTeamIdFromProcessId(String processId, String provider, String region, String superCluster) {
    try {
      String teamId = cloudInstanceHelperRepository.getTeamIdFromProcessId(processId, provider, region, superCluster);
      return teamId;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling getTeamIdFromProcessId: " + e.getMessage());
      return null;
    }
  }

  public Boolean doesCloudInstanceExist(Organization organization, String teamId, String cloudInstanceName,
      String provider, String region, String superCluster) {
    try {
      Boolean doesCloudInstanceExist = cloudInstanceHelperRepository.doesCloudInstanceExist(organization, teamId,
          cloudInstanceName, provider, region, superCluster);
      System.out.println("Cloud instance exist: " + doesCloudInstanceExist);
      return doesCloudInstanceExist;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling doesCloudInstanceExist: " + e.getMessage());
      return null;
    }
  }

  public String getAvailableCIDRBlock(String provider, String region, String superCluster) {
    try {
      String availableCIDRBlock = cloudInstanceHelperRepository.getAvailableCIDRBlock(provider, region, superCluster);
      return availableCIDRBlock;
    } catch (Exception e) {
      cloudInstanceHelperLogger.error("Error while calling getAvailableCIDRBlock: " + e.getMessage());
      return null;
    }
  }
}
