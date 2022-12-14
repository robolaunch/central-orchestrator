package org.robolaunch.service;

import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.models.Organization;
import org.robolaunch.models.Provider;
import org.robolaunch.models.RegionKubernetes;
import org.robolaunch.models.RoboticsCloudKubernetes;
import org.robolaunch.models.SuperClusterKubernetes;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.models.response.ResponseProviders;
import org.robolaunch.models.response.ResponseRegions;
import org.robolaunch.models.response.ResponseRoboticsClouds;
import org.robolaunch.models.response.ResponseSuperClusters;
import org.robolaunch.repository.abstracts.KubernetesRepository;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class KubernetesService {

  @Inject
  Logger logger;

  @Inject
  KubernetesRepository kubernetesRepository;

  @Inject
  StorageService storageService;

  @Inject
  JsonWebToken jwt;

  @LoggerName("kubernetesService")
  Logger kubernetesLogger;

  public Integer getDesiredBufferCountOfType(String instanceType, String provider, String region, String superCluster) {
    try {
      String getContent = storageService.getSuperClusterContent(provider, region, superCluster);
      String lines[] = getContent.split("\\r?\\n");

      for (String line : lines) {
        String[] row = line.split(":");
        String type = row[0];
        if (type.equals(instanceType)) {
          String value = row[1];
          return Integer.parseInt(value);
        }
      }

      return null;
    } catch (Exception e) {
      kubernetesLogger.error("Error while getting cloud instances desired from kubernetes buffer", e);
    }
    return 0;
  }

  public Integer getCurrentBufferCountOfType(String instanceType, String provider, String region, String superCluster) {
    try {
      // Get buffered count
      Integer bufferedCount = kubernetesRepository.getCurrentBufferedCountOfType(instanceType, provider, region,
          superCluster);

      // Get buffering count
      Integer bufferingCount = kubernetesRepository.getCurrentBufferingCountOfType(instanceType, provider, region,
          superCluster);
      return bufferingCount + bufferedCount;
    } catch (Exception e) {
      kubernetesLogger.error("Error while getting cloud instances current from kubernetes buffer", e);
      return null;
    }
  }

  public Integer getReadyBufferCount(String instanceType, String provider, String region, String superCluster) {
    try {
      // Get buffering count
      Integer bufferedCount = kubernetesRepository.getCurrentBufferedCountOfType(instanceType, provider, region,
          superCluster);
      return bufferedCount;
    } catch (Exception e) {
      kubernetesLogger.error("Error while getting cloud instances ready from kubernetes buffer", e);
      return null;
    }
  }

  public String getSuperClusterProcessId(String provider, String region, String superCluster) {
    try {
      String processId = kubernetesRepository.getSuperClusterProcessId(provider, region, superCluster);
      return processId;
    } catch (Exception e) {
      return null;
    }
  }

  public String checkIfTypeNeedsBuffer(String provider, String region,
      String superCluster) {
    ArrayList<String> types = storageService.getSuperClusterBufferTypes(provider, region, superCluster);
    try {
      for (String type : types) {
        Integer desiredBufferCount = getDesiredBufferCountOfType(type, provider, region, superCluster);
        Integer currentBufferCount = getCurrentBufferCountOfType(type, provider, region, superCluster);
        if (desiredBufferCount > currentBufferCount) {
          return type;
        }
      }
      return "";
    } catch (Exception e) {
      return null;
    }
  }

  public PlainResponse providerExists(String provider) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      Boolean exists = kubernetesRepository.providerExists(provider);
      if (exists) {
        plainResponse.setSuccess(true);
        plainResponse.setMessage("Not created. Provider already exists: " + exists);
      } else {
        plainResponse.setSuccess(false);
        plainResponse.setMessage("Provider does not exist. Will be created now.");
      }
      return plainResponse;
    } catch (Exception e) {
      return plainResponse;
    }
  }

  public PlainResponse regionExists(String provider, String region) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      Boolean exists = kubernetesRepository.regionExists(provider, region);
      if (exists) {
        plainResponse.setSuccess(true);
        plainResponse.setMessage("Not created. Provider already exists: " + exists);
      } else {
        plainResponse.setSuccess(false);
        plainResponse.setMessage("Provider does not exist. Will be created now.");
      }
      return plainResponse;
    } catch (Exception e) {
      return plainResponse;
    }
  }

  public ResponseProviders getProviders() {
    ResponseProviders responseProviders = new ResponseProviders();
    try {
      ArrayList<Provider> providers = kubernetesRepository.getProviders();
      responseProviders.setData(providers);
      responseProviders.setSuccess(true);
      responseProviders.setMessage("Providers fetched successfully.");
    } catch (Exception e) {
      responseProviders.setSuccess(false);
      responseProviders.setMessage("Error while fetching providers." + e.getMessage());
    }
    return responseProviders;

  }

  public ResponseRegions getRegions(String provider) {
    ResponseRegions responseRegions = new ResponseRegions();
    try {
      ArrayList<RegionKubernetes> regions = kubernetesRepository.getRegions(provider);
      responseRegions.setData(regions);
      responseRegions.setSuccess(true);
      responseRegions.setMessage("regions fetched successfully.");
    } catch (Exception e) {
      responseRegions.setSuccess(false);
      responseRegions.setMessage("Error while fetching regions." + e.getMessage());
    }
    return responseRegions;
  }

  public ResponseSuperClusters getSuperClusters(String provider, String region) {
    ResponseSuperClusters responseSuperClusters = new ResponseSuperClusters();
    try {
      ArrayList<SuperClusterKubernetes> superClusters = kubernetesRepository.getSuperClusters(provider, region);
      responseSuperClusters.setData(superClusters);
      responseSuperClusters.setSuccess(true);
      responseSuperClusters.setMessage("regions fetched successfully.");
    } catch (Exception e) {
      responseSuperClusters.setSuccess(false);
      responseSuperClusters.setMessage("Error while fetching regions." + e.getMessage());
    }
    return responseSuperClusters;
  }

  public ResponseRoboticsClouds getRoboticsCloudsOrganization(Organization organization) {
    ResponseRoboticsClouds responseRoboticsClouds = new ResponseRoboticsClouds();
    try {
      ArrayList<RoboticsCloudKubernetes> roboticsClouds = kubernetesRepository
          .getRoboticsCloudsOrganization(organization);
      responseRoboticsClouds.setData(roboticsClouds);
      responseRoboticsClouds.setSuccess(true);
      responseRoboticsClouds.setMessage("RoboticsClouds fetched successfully.");
    } catch (Exception e) {
      responseRoboticsClouds.setSuccess(false);
      responseRoboticsClouds.setMessage("Error while fetching RoboticsClouds." + e.getMessage());
    }
    return responseRoboticsClouds;
  }

  public ResponseRoboticsClouds getRoboticsCloudsTeam(Organization organization, String teamId) {
    ResponseRoboticsClouds responseRoboticsClouds = new ResponseRoboticsClouds();
    try {
      ArrayList<RoboticsCloudKubernetes> roboticsClouds = kubernetesRepository
          .getRoboticsCloudsTeam(organization, teamId);
      responseRoboticsClouds.setData(roboticsClouds);
      responseRoboticsClouds.setSuccess(true);
      responseRoboticsClouds.setMessage("RoboticsClouds fetched successfully.");
    } catch (Exception e) {
      responseRoboticsClouds.setSuccess(false);
      responseRoboticsClouds.setMessage("Error while fetching RoboticsClouds." + e.getMessage());
    }
    return responseRoboticsClouds;
  }

  public ResponseRoboticsClouds getRoboticsCloudsUser(Organization organization, String teamId) {
    ResponseRoboticsClouds responseRoboticsClouds = new ResponseRoboticsClouds();
    String username = jwt.getClaim("preferred_username");
    try {
      ArrayList<RoboticsCloudKubernetes> roboticsClouds = kubernetesRepository
          .getRoboticsCloudsUser(organization, teamId, username);
      responseRoboticsClouds.setData(roboticsClouds);
      responseRoboticsClouds.setSuccess(true);
      responseRoboticsClouds.setMessage("RoboticsClouds fetched successfully.");
    } catch (Exception e) {
      responseRoboticsClouds.setSuccess(false);
      responseRoboticsClouds.setMessage("Error while fetching RoboticsClouds." + e.getMessage());
    }
    return responseRoboticsClouds;
  }

}