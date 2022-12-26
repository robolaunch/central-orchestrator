package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.jboss.logging.Logger;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
import org.robolaunch.repository.abstracts.CloudInstanceRepository;

import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiException;
import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class CloudInstanceService {
  @Inject
  Logger log;

  @Inject
  CloudInstanceRepository cloudInstanceRepository;

  @Inject
  CloudInstanceHelperService cloudInstanceHelperService;

  @LoggerName("cloudInstanceService")
  Logger cloudInstanceLogger;

  public Response createMachineDeployment(String bufferName, String instanceType, String region) {
    try {
      cloudInstanceLogger.info("Machine deployment created -> " + bufferName);
      cloudInstanceRepository.createMachineDeployment(bufferName, instanceType, region);
      return new Response(true, "Machine deployment created successfully");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating machine deployment", e);
      return new Response(false, "Error while creating machine deployment");
    }
  }

  public Response claimTheSuperClusterNode(String nodeName, String bufferName, String region) {
    try {
      cloudInstanceRepository.claimTheSuperClusterNode(nodeName, bufferName, region);
      cloudInstanceLogger.info("Label added to node");
      return new Response(true, "Label added to node successfully");
    } catch (KubectlException e) {
      cloudInstanceLogger.error("Error while adding label to node - kubectlexception", e);
      return new Response(false, "Error while adding label to node");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while adding label to node - exception", e);
      return new Response(false, "Error while adding label to node");
    }
  }

  public Response createClusterVersion(String bufferName, String region) {
    try {
      cloudInstanceRepository.createClusterVersion(bufferName, region);
      cloudInstanceLogger.info("Cluster version created");

      return new Response(true, "Cluster version created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating cluster version.", e);
      return new Response(false, "Error while creating cluster version.");
    }
  }

  public Response createVirtualCluster(String bufferName, String region) {
    try {
      cloudInstanceRepository.createVirtualCluster(bufferName, region);
      cloudInstanceLogger.info("Virtual cluster created");
      return new Response(true, "Virtual cluster created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating virtual cluster.", e);
      return new Response(false, "Error while creating virtual cluster.");
    }
  }

  public Response scaleStatefulSetsDown(String bufferName, String region) {
    try {
      cloudInstanceRepository.scaleStatefulSetsDown(bufferName, region);
      cloudInstanceLogger.info("Resources scaled");
      return new Response(true, "Resources scaled.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling resources.", e);
      return new Response(false, "Error while scaling resources.");
    }
  }

  public Boolean mustBeAnEndNode() {
    return true;
  }

  public Response labelVirtualCluster(String bufferName, Organization organization, String teamId,
      String region, String cloudInstanceName, Boolean connectionHub) {
    try {
      cloudInstanceRepository.labelVirtualCluster(bufferName, organization, teamId,
          region, cloudInstanceName, connectionHub);
      cloudInstanceLogger.info("VC labeled");
      return new Response(true, "VC labeled.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while labeling VC.", e);
      return new Response(false, "Error labeling VC.");

    }
  }

  public Response addOrganizationLabelsToNode(Organization organization, String nodeName, String cloudInstanceName,
      String teamId, String region, Boolean connectionHub) {
    try {
      cloudInstanceRepository.addOrganizationLabelsToNode(organization, nodeName, cloudInstanceName, teamId,
          region, connectionHub);
      cloudInstanceLogger.info("SC Node labeled");
      return new Response(true, "SC Node labeled.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while labeling SC Node.", e);
      return new Response(false, "SC Node labeled.");

    }
  }

  public Response addNodeSelectorsToStatefulSets(String namespaceName, Organization organization,
      String teamId,
      String cloudInstanceName, String region, String bufferName) {
    try {
      cloudInstanceRepository.addNodeSelectorsToStatefulSets(namespaceName, organization,
          teamId,
          cloudInstanceName, region, bufferName);
      cloudInstanceLogger.info("Node selectors added to statefulsets");
      return new Response(true, "Node selectors added to statefulsets.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while adding node selectors to statefulsets.", e);
      return new Response(true, "Error while adding node selectors to statefulsets.");

    }
  }

  public Response scaleStatefulSetsUp(String bufferName, String region) {
    try {
      cloudInstanceRepository.scaleStatefulSetsUp(bufferName, region);
      cloudInstanceLogger.info("Resources scaled up");
      return new Response(true, "Resources scaled up.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling resources up.", e);
      return new Response(false, "Error while scaling resources up.");
    }
  }

  public Response createSubnet(String bufferName, String namespaceName, String cloudInstanceName,
      String teamId, Organization organization, String region) {
    try {
      cloudInstanceRepository.createSubnet(bufferName, namespaceName, cloudInstanceName,
          teamId, organization, region);
      cloudInstanceLogger.info("Subnet created");
      return new Response(true, "Subnet created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating subnet.", e);
      return new Response(false, "Error creating subnet.");

    }
  }

  public Response createOauth2ProxyNamespace(String bufferName, String region) {
    try {
      cloudInstanceRepository.createOauth2ProxyNamespace(bufferName, region);
      cloudInstanceLogger.info("Oauth2 proxy namespace created");
      return new Response(true, "Oauth2 proxy namespace created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating oauth2 proxy namespace.", e);
      return new Response(false, "Error creating oauth2 proxy namespace.");

    }
  }

  public Response createTLSSecrets(String bufferName, String region) {
    try {
      cloudInstanceRepository.createTLSSecrets(bufferName, region);
      cloudInstanceLogger.info("TLS secrets created");
      return new Response(true, "TLS secrets created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating TLS secrets.", e);
      return new Response(true, "Error creating TLS secrets.");

    }
  }

  public Response createOAuth2ProxyResources(Organization organization, String teamId,
      String cloudInstanceName, String region, String namespaceName, String bufferName) {
    try {
      cloudInstanceRepository.createOAuth2ProxyResources(organization, teamId, cloudInstanceName,
          region, namespaceName, bufferName);
      cloudInstanceLogger.info("OAuth2 proxy resources created");
      return new Response(true, "OAuth2 proxy resources created.");
    } catch (ApiException e) {
      cloudInstanceLogger.error("Error while creating OAuth2 proxy resources.", e);
      return new Response(false, "Error creating OAuth2 proxy resources.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating OAuth2 proxy resources.", e);
      return new Response(false, "Error creating OAuth2 proxy resources.");
    }
  }

  public Response createIngressRule(Organization organization, String cloudInstanceName,
      String bufferName, String region) {
    try {
      cloudInstanceRepository.createIngressRule(organization, cloudInstanceName, bufferName, region);
      cloudInstanceLogger.info("Ingress rule created");
      return new Response(true, "Ingress rule created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating ingress rule.", e);
      return new Response(false, "Error creating ingress rule.");

    }
  }

  public Response scaleVCWorkloadsUp(String bufferName, String region) {
    try {
      cloudInstanceRepository.scaleVCWorkloadsUp(bufferName, region);
      cloudInstanceLogger.info("VC workloads scaled to one");
      return new Response(true, "VC workloads scaled to one.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling VC workloads to one.", e);
      return new Response(false, "Error scaling VC workloads to one.");
    }
  }

  public Response scaleOAuth2ProxyDown(String bufferName, String region) {
    try {
      cloudInstanceRepository.scaleOAuth2ProxyDown(bufferName, region);
      cloudInstanceLogger.info("OAuth2 Proxy scaled to zero");
      return new Response(true, "OAuth2 Proxy scaled to zero.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling OAuth2 Proxy to zero.", e);
      return new Response(false, "Error scaling OAuth2 Proxy to zero.");
    }
  }

  public Response createCoreDNS(Organization organization, String teamId,
      String cloudInstanceName, String nodeName, String bufferName, String region) {
    try {
      cloudInstanceRepository.createCoreDNS(organization, teamId,
          cloudInstanceName, nodeName, bufferName, region);
      cloudInstanceLogger.info("CoreDNS created");
      return new Response(true, "CoreDNS created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating CoreDNS.", e);
      return new Response(false, "Error creating CoreDNS.");
    }
  }

  public Response addLabelsToVirtualClusterNode(Organization organization, String nodeName, String cloudInstanceName,
      String teamId, String bufferName, String region, Boolean connectionHub) {
    try {
      cloudInstanceRepository.addLabelsToVirtualClusterNode(organization, nodeName, cloudInstanceName,
          teamId, bufferName, region, connectionHub);
      cloudInstanceLogger.info("Label added to virtual cluster node");
      return new Response(true, "Label added to virtual cluster node.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while adding label to virtual cluster node.", e);
      return new Response(false, "Error adding label to virtual cluster node.");
    }
  }

  public Response createCertManager(Organization organization, String teamId,
      String cloudInstanceName, String bufferName, String region) {
    try {
      cloudInstanceRepository.createCertManager(organization, teamId,
          cloudInstanceName, bufferName, region);
      cloudInstanceLogger.info("Cert manager created");
      return new Response(true, "Cert manager created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating cert manager.", e);
      return new Response(false, "Error creating cert manager.");
    }
  }

  public Response createRobotOperator(Organization organization, String cloudInstanceName,
      String teamId, String region, String bufferName) {
    try {
      cloudInstanceRepository.createRobotOperator(organization, cloudInstanceName,
          teamId, region, bufferName);
      Thread.sleep(5000);
      cloudInstanceLogger.info("Robot operator created");
      return new Response(true, "Robot operator created.");
    } catch (ApiException e) {
      System.out.println("Catch api exception");
      System.out.println(e.getCode());
      System.out.println(e.getResponseBody());
      cloudInstanceLogger.error("Error while creating robot operator.", e);
      return new Response(false, "Error creating robot operator.");
    } catch (Exception e) {
      System.out.println("Catch Normal Exception");
      System.out.println(e.getCause());
      System.out.println(e.getMessage());
      cloudInstanceLogger.error("Error while creating robot operator.", e);
      return new Response(false, "Error creating robot operator.");
    }
  }

  public Response drainNode(String nodeName, String region) {
    try {
      cloudInstanceRepository.drainNode(nodeName, region);
      cloudInstanceLogger.info("Node drained");
      return new Response(true, "Node drained.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while draining node.", e);
      return new Response(false, "Error draining node.");
    }
  }

  public Response uncordonNode(String nodeName, String region) {
    try {
      cloudInstanceRepository.uncordonNode(nodeName, region);
      cloudInstanceLogger.info("Node uncordoned");
      return new Response(true, "Node uncordoned.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while uncordoning node.", e);
      return new Response(false, "Error uncordoning node.");
    }
  }

  public Response createDNSRecord(Organization organization, String nodeName, String region) {
    try {
      cloudInstanceRepository.createDNSRecord(organization, nodeName, region);
      cloudInstanceLogger.info("DNS Record created");
      return new Response(true, "DNS Record created.");
    } catch (Exception e) {
      return new Response(false, "Error creating DNS Record.");
    }
  }

  public void addBufferedLabelToVC(String bufferName, String instanceType, String region) {
    try {
      cloudInstanceRepository.addBufferedLabelToVC(bufferName, instanceType, region);
      cloudInstanceLogger.info("Buffered label added to VC");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while adding buffered label to VC.", e);
    }
  }

  public Response scaleVCWorkloadsDown(String bufferName, String region) {
    try {
      cloudInstanceRepository.scaleVCWorkloadsDown(bufferName, region);
      cloudInstanceLogger.info("VC workloads scaled to zero");
      return new Response(true, "VC workloads scaled to zero.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling VC workloads to zero.", e);
      return new Response(false, "Error scaling VC workloads to zero.");
    }
  }

  public Response scaleOAuth2ProxyUp(String bufferName, String region) {
    try {
      cloudInstanceRepository.scaleOAuth2ProxyUp(bufferName, region);
      cloudInstanceLogger.info("OAuth2 Proxy scaled to one");
      return new Response(true, "OAuth2 Proxy scaled to one.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling OAuth2 Proxy to one.", e);
      return new Response(false, "Error scaling OAuth2 Proxy to one.");
    }
  }

  public Response scaleCoreDNSUp(String bufferName, String region) {
    try {
      cloudInstanceRepository.scaleCoreDNSUp(bufferName, region);
      cloudInstanceLogger.info("CoreDNS scaled to one");
      return new Response(true, "CoreDNS scaled to one.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling CoreDNS to one.", e);
      return new Response(false, "Error scaling CoreDNS to one.");
    }
  }

  public Response unlabelSuperClusterNode(String nodeName, String region) {
    try {
      cloudInstanceRepository.unlabelSuperClusterNode(nodeName, region);
      cloudInstanceLogger.info("Node unlabelled");
      return new Response(true, "Node unlabelled.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while unlabelling node.", e);
      return new Response(false, "Error unlabelling node.");
    }
  }

  public Response createVirtualLink(String namespaceName, String cloudInstanceName,
      String teamId, Organization organization, String region, String bufferName) {
    try {
      cloudInstanceRepository.createVirtualLink(namespaceName, cloudInstanceName,
          teamId, organization, region, bufferName);
      cloudInstanceLogger.info("Virtual link created");
      return new Response(true, "Virtual link created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating virtual link.", e);
      return new Response(false, "Error creating virtual link.");
    }
  }

  public Response createConnectionHubOperator(String namespaceName, String cloudInstanceName,
      String teamId, Organization organization, String region, String bufferName) {
    try {
      cloudInstanceRepository.createConnectionHubOperator(namespaceName, cloudInstanceName,
          teamId, organization, region, bufferName);
      cloudInstanceLogger.info("Connection hub operator created");
      return new Response(true, "Connection hub operator created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating connection hub operator.", e);
      return new Response(false, "Error creating connection hub operator.");
    }
  }

  public Response createConnectionHub(String bufferName, Organization organization, String teamId,
      String cloudInstanceName, String namespaceName, String region) {
    try {
      String serverIP = cloudInstanceHelperService.getCloudInstanceIP(bufferName, region);
      cloudInstanceRepository.createConnectionHub(bufferName, organization, teamId,
          cloudInstanceName, serverIP, namespaceName, region);
      cloudInstanceLogger.info("Connection hub created");
      return new Response(true, "Connection hub created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating connection hub.", e);
      return new Response(false, "Error creating connection hub.");
    }
  }

  public Response createClusterAdminRole(Organization organization, String bufferName, String username, String region) {
    try {
      cloudInstanceRepository.createClusterAdminRole(organization, bufferName, username, region);
      cloudInstanceLogger.info("Cluster admin role created");
      return new Response(true, "Cluster admin role created.");
    } catch (ApiException e) {
      return new Response(false, "Cluster admin role cannot be created.");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating cluster admin role.", e);
      return new Response(false, "Error creating cluster admin role.");
    }
  }

}
