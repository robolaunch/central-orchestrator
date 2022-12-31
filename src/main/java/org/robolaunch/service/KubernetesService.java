package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.models.Organization;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.repository.abstracts.KubernetesRepository;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class KubernetesService {

  @Inject
  Logger logger;

  @Inject
  KubernetesRepository kubernetesRepository;

  @Inject
  JsonWebToken jwt;

  @LoggerName("kubernetesService")
  Logger kubernetesLogger;

  public PlainResponse getCloudInstances(Organization organization, String teamId) {
    PlainResponse response = new PlainResponse();
    try {
      kubernetesRepository.getCloudInstances(organization, teamId);
      return null;
    } catch (Exception e) {
      kubernetesLogger.error("Error while getting cloud instances from kubernetes buffer", e);
    }
    return response;
  }

  public Integer getBufferInstanceCount(String provider, String region, String superCluster) {
    try {
      kubernetesRepository.getBufferInstanceCount(provider, region, superCluster);
      return null;
    } catch (Exception e) {
      kubernetesLogger.error("Error while getting cloud instances from kubernetes buffer", e);
    }
    return 0;
  }

}
