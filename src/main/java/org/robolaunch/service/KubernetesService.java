package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.models.Organization;
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

  public void getCloudInstances(Organization organization, String teamId) {
    try {
      kubernetesRepository.getCloudInstances(organization, teamId);
    } catch (Exception e) {
      kubernetesLogger.error("Error while getting cloud instances from kubernetes buffer", e);
    }
  }

}
