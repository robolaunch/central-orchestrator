package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.jboss.logging.Logger;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Response;
import org.robolaunch.models.response.PlainResponse;
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

  public PlainResponse createMachineDeployment(String bufferName, String instanceType, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceLogger.info("Machine deployment created -> " + bufferName);
      cloudInstanceRepository.createMachineDeployment(bufferName, instanceType, region);
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating machine deployment", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse claimTheSuperClusterNode(String nodeName, String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.claimTheSuperClusterNode(nodeName, bufferName, region);
      cloudInstanceLogger.info("Label added to node");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while adding label to node - exception", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createClusterVersion(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createClusterVersion(bufferName, region);
      cloudInstanceLogger.info("Cluster version created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating cluster version.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createVirtualCluster(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createVirtualCluster(bufferName, region);
      cloudInstanceLogger.info("Virtual cluster created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating virtual cluster.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse scaleStatefulSetsDown(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.scaleStatefulSetsDown(bufferName, region);
      cloudInstanceLogger.info("Resources scaled");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling resources.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public Boolean mustBeAnEndNode() {
    return true;
  }

  public PlainResponse labelVirtualCluster(String bufferName, Organization organization, String teamId,
      String region, String cloudInstanceName, Boolean connectionHub) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.labelVirtualCluster(bufferName, organization, teamId,
          region, cloudInstanceName, connectionHub);
      plainResponse.setSuccess(true);
      cloudInstanceLogger.info("VC labeled");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while labeling VC.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse addOrganizationLabelsToNode(Organization organization, String nodeName, String cloudInstanceName,
      String teamId, String region, Boolean connectionHub) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.addOrganizationLabelsToNode(organization, nodeName, cloudInstanceName, teamId,
          region, connectionHub);
      plainResponse.setSuccess(true);
      cloudInstanceLogger.info("SC Node labeled");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while labeling SC Node.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse addNodeSelectorsToStatefulSets(String namespaceName, Organization organization,
      String teamId,
      String cloudInstanceName, String region, String bufferName) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.addNodeSelectorsToStatefulSets(namespaceName, organization,
          teamId,
          cloudInstanceName, region, bufferName);
      plainResponse.setSuccess(true);
      cloudInstanceLogger.info("Node selectors added to statefulsets");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while adding node selectors to statefulsets.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse scaleStatefulSetsUp(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.scaleStatefulSetsUp(bufferName, region);
      plainResponse.setSuccess(true);
      cloudInstanceLogger.info("Resources scaled up");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling resources up.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createSubnet(String bufferName, String namespaceName, String cloudInstanceName,
      String teamId, Organization organization, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createSubnet(bufferName, namespaceName, cloudInstanceName,
          teamId, organization, region);
      plainResponse.setSuccess(true);
      cloudInstanceLogger.info("Subnet created");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating subnet.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createOauth2ProxyNamespace(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createOauth2ProxyNamespace(bufferName, region);
      cloudInstanceLogger.info("Oauth2 proxy namespace created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating oauth2 proxy namespace.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createTLSSecrets(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createTLSSecrets(bufferName, region);
      cloudInstanceLogger.info("TLS secrets created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating TLS secrets.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createOAuth2ProxyResources(Organization organization, String teamId,
      String cloudInstanceName, String region, String namespaceName, String bufferName) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createOAuth2ProxyResources(organization, teamId, cloudInstanceName,
          region, namespaceName, bufferName);
      plainResponse.setSuccess(true);
      cloudInstanceLogger.info("OAuth2 proxy resources created");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating OAuth2 proxy resources.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createIngressRule(Organization organization, String cloudInstanceName,
      String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createIngressRule(organization, cloudInstanceName, bufferName, region);
      cloudInstanceLogger.info("Ingress rule created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating ingress rule.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse scaleVCWorkloadsUp(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.scaleVCWorkloadsUp(bufferName, region);
      cloudInstanceLogger.info("VC workloads scaled to one");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling VC workloads to one.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse scaleOAuth2ProxyDown(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.scaleOAuth2ProxyDown(bufferName, region);
      cloudInstanceLogger.info("OAuth2 Proxy scaled to zero");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling OAuth2 Proxy to zero.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createCoreDNS(Organization organization, String teamId,
      String cloudInstanceName, String nodeName, String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createCoreDNS(organization, teamId,
          cloudInstanceName, nodeName, bufferName, region);
      cloudInstanceLogger.info("CoreDNS created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating CoreDNS.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse addLabelsToVirtualClusterNode(Organization organization, String nodeName,
      String cloudInstanceName,
      String teamId, String bufferName, String region, Boolean connectionHub) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.addLabelsToVirtualClusterNode(organization, nodeName, cloudInstanceName,
          teamId, bufferName, region, connectionHub);
      cloudInstanceLogger.info("Label added to virtual cluster node");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while adding label to virtual cluster node.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createCertManager(Organization organization, String teamId,
      String cloudInstanceName, String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createCertManager(organization, teamId,
          cloudInstanceName, bufferName, region);
      cloudInstanceLogger.info("Cert manager created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating cert manager.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createRobotOperator(Organization organization, String cloudInstanceName,
      String teamId, String region, String bufferName) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createRobotOperator(organization, cloudInstanceName,
          teamId, region, bufferName);
      plainResponse.setSuccess(true);
      cloudInstanceLogger.info("Robot operator created");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating robot operator.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse drainNode(String nodeName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.drainNode(nodeName, region);
      cloudInstanceLogger.info("Node drained");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while draining node.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse uncordonNode(String nodeName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.uncordonNode(nodeName, region);
      cloudInstanceLogger.info("Node uncordoned");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while uncordoning node.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createDNSRecord(Organization organization, String nodeName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createDNSRecord(organization, nodeName, region);
      cloudInstanceLogger.info("DNS Record created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse addBufferedLabelToVC(String bufferName, String instanceType, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.addBufferedLabelToVC(bufferName, instanceType, region);
      plainResponse.setSuccess(true);
      cloudInstanceLogger.info("Buffered label added to VC");
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while adding buffered label to VC.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse scaleVCWorkloadsDown(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.scaleVCWorkloadsDown(bufferName, region);
      cloudInstanceLogger.info("VC workloads scaled to zero");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling VC workloads to zero.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse scaleOAuth2ProxyUp(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.scaleOAuth2ProxyUp(bufferName, region);
      cloudInstanceLogger.info("OAuth2 Proxy scaled to one");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling OAuth2 Proxy to one.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse scaleCoreDNSUp(String bufferName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.scaleCoreDNSUp(bufferName, region);
      cloudInstanceLogger.info("CoreDNS scaled to one");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while scaling CoreDNS to one.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse unlabelSuperClusterNode(String nodeName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.unlabelSuperClusterNode(nodeName, region);
      cloudInstanceLogger.info("Node unlabelled");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while unlabelling node.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createVirtualLink(String namespaceName, String cloudInstanceName,
      String teamId, Organization organization, String region, String bufferName) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createVirtualLink(namespaceName, cloudInstanceName,
          teamId, organization, region, bufferName);
      cloudInstanceLogger.info("Virtual link created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating virtual link.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createConnectionHubOperator(String namespaceName, String cloudInstanceName,
      String teamId, Organization organization, String region, String bufferName) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createConnectionHubOperator(namespaceName, cloudInstanceName,
          teamId, organization, region, bufferName);
      cloudInstanceLogger.info("Connection hub operator created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating connection hub operator.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createConnectionHub(String bufferName, Organization organization, String teamId,
      String cloudInstanceName, String namespaceName, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      String serverIP = cloudInstanceHelperService.getCloudInstanceIP(bufferName, region);
      cloudInstanceRepository.createConnectionHub(bufferName, organization, teamId,
          cloudInstanceName, serverIP, namespaceName, region);
      cloudInstanceLogger.info("Connection hub created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating connection hub.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createClusterAdminRole(Organization organization, String bufferName, String username,
      String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      cloudInstanceRepository.createClusterAdminRole(organization, bufferName, username, region);
      cloudInstanceLogger.info("Cluster admin role created");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      cloudInstanceLogger.error("Error while creating cluster admin role.", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

}
