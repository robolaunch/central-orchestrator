package org.robolaunch.service;

import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.models.Fleet;
import org.robolaunch.models.Organization;
import org.robolaunch.models.PhysicalInstanceKubernetes;
import org.robolaunch.models.ProviderKubernetes;
import org.robolaunch.models.RegionKubernetes;
import org.robolaunch.models.Robot;
import org.robolaunch.models.RoboticsCloudKubernetes;
import org.robolaunch.models.SuperClusterKubernetes;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.models.response.ResponseFleets;
import org.robolaunch.models.response.ResponsePhysicalInstances;
import org.robolaunch.models.response.ResponseProviders;
import org.robolaunch.models.response.ResponseRegions;
import org.robolaunch.models.response.ResponseRoboticsClouds;
import org.robolaunch.models.response.ResponseRobots;
import org.robolaunch.models.response.ResponseSuperClusters;
import org.robolaunch.repository.abstracts.KubernetesRepository;

import com.google.gson.JsonObject;

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
        kubernetesLogger
            .info("Checking if type needs buffer: " + type + " desired: " + desiredBufferCount + " current: "
                + currentBufferCount + "");
        if (desiredBufferCount > currentBufferCount) {
          return type;
        }
      }
      return "";
    } catch (Exception e) {
      kubernetesLogger.error("Error checking if needs buffer.");
      return "";
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
      return null;
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
      ArrayList<ProviderKubernetes> providers = kubernetesRepository.getProviders();
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

  public String readPlatformContent(String version, String resource) {
    try {
      String platformContent = kubernetesRepository.readPlatformContent(version, resource);
      kubernetesLogger.info("Platform Content is read successfully.");
      return platformContent;
    } catch (Exception e) {
      kubernetesLogger.error("Error while reading Platform Content: " + e.getMessage());
      return null;
    }
  }

  public JsonObject readPlatformContentAsJsonObject(String version, String resource) {
    try {
      JsonObject platformContent = kubernetesRepository.readPlatformContentAsJsonObject(version, resource);
      kubernetesLogger.info("Platform Object is read successfully.");
      return platformContent;
    } catch (Exception e) {
      kubernetesLogger.error("Error while reading Platform Object: " + e.getMessage());
      return null;
    }
  }

  public String getLatestPlatformVersion() {
    try {
      String platformVersion = kubernetesRepository.getLatestPlatformVersion();
      kubernetesLogger.info("Latest platform version: " + platformVersion);
      return platformVersion;
    } catch (Exception e) {
      kubernetesLogger.error("Cannot fetch platform version.");
      return null;
    }
  }

  public Boolean isAuthorizedRoboticsCloud(Organization organization, String teamId) {
    try {
      String username = jwt.getClaim("preferred_username");
      Boolean isAuthorized = kubernetesRepository.isAuthorizedRoboticsCloud(organization, teamId, username);
      kubernetesLogger.info("Checked if user is authorized.");
      return isAuthorized;
    } catch (Exception e) {
      kubernetesLogger.error("Cannot check if authorized.");
      return null;
    }
  }

  public ResponseRobots getRobotsOrganization(Organization organization) {
    ResponseRobots responseRobots = new ResponseRobots();
    try {
      ArrayList<Robot> robots = kubernetesRepository
          .getRobotsOrganization(organization);
      responseRobots.setData(robots);
      responseRobots.setSuccess(true);
      responseRobots.setMessage("Robots fetched successfully.");
      kubernetesLogger.info("Robots fetched successfully.");
    } catch (Exception e) {
      responseRobots.setSuccess(false);
      responseRobots.setMessage("Error while fetching Robots.");
      kubernetesLogger.error("Error while fetching Robots." + e.getMessage());
    }
    return responseRobots;
  }

  public ResponseRobots getRobotsTeam(Organization organization, String teamId) {
    ResponseRobots responseRobots = new ResponseRobots();
    try {
      ArrayList<Robot> robots = kubernetesRepository
          .getRobotsTeam(organization, teamId);
      responseRobots.setData(robots);
      responseRobots.setSuccess(true);
      responseRobots.setMessage("Robots fetched successfully.");
      kubernetesLogger.info("Robots fetched successfully.");
    } catch (Exception e) {
      responseRobots.setSuccess(false);
      responseRobots.setMessage("Error while fetching Robots." + e.getMessage());
      kubernetesLogger.error("Error while fetching Robots." + e.getMessage());

    }
    return responseRobots;
  }

  public ResponseRobots getRobotsRoboticsCloud(String roboticsCloudProcessId) {
    ResponseRobots responseRobots = new ResponseRobots();
    try {
      ArrayList<Robot> robots = kubernetesRepository
          .getRobotsRoboticsCloud(roboticsCloudProcessId);
      responseRobots.setData(robots);
      responseRobots.setSuccess(true);
      responseRobots.setMessage("Robots fetched successfully.");
      kubernetesLogger.info("Robots fetched successfully.");

    } catch (Exception e) {
      responseRobots.setSuccess(false);
      responseRobots.setMessage("Error while fetching Robots." + e.getMessage());
      kubernetesLogger.error("Error while fetching Robots." + e.getMessage());
    }
    return responseRobots;
  }

  public ResponseRobots getRobotsFleet(String fleetProcessId) {
    ResponseRobots responseRobots = new ResponseRobots();
    try {
      ArrayList<Robot> robots = kubernetesRepository
          .getRobotsFleet(fleetProcessId);
      responseRobots.setData(robots);
      responseRobots.setSuccess(true);
      responseRobots.setMessage("Robots fetched successfully.");
      kubernetesLogger.info("Robots fetched successfully.");
    } catch (Exception e) {
      responseRobots.setSuccess(false);
      responseRobots.setMessage("Error while fetching Robots." + e.getMessage());
      kubernetesLogger.error("Error while fetching Robots." + e.getMessage());
    }
    return responseRobots;
  }

  public ResponseFleets getFleetsOrganization(Organization organization) {
    ResponseFleets responseFleets = new ResponseFleets();
    try {
      ArrayList<Fleet> fleets = kubernetesRepository
          .getFleetsOrganization(organization);
      responseFleets.setData(fleets);
      responseFleets.setSuccess(true);
      responseFleets.setMessage("Fleets fetched successfully.");
      kubernetesLogger.info("Fleets fetched successfully.");
    } catch (Exception e) {
      responseFleets.setSuccess(false);
      responseFleets.setMessage("Error while fetching Fleets.");
      kubernetesLogger.error("Error while fetching Fleets.");
    }
    return responseFleets;
  }

  public ResponseFleets getFleetsTeam(Organization organization, String teamId) {
    ResponseFleets responseFleets = new ResponseFleets();
    try {
      ArrayList<Fleet> fleets = kubernetesRepository
          .getFleetsTeam(organization, teamId);
      responseFleets.setData(fleets);
      responseFleets.setSuccess(true);
      responseFleets.setMessage("Fleets fetched successfully.");
      kubernetesLogger.info("Fleets fetched successfully.");
    } catch (Exception e) {
      responseFleets.setSuccess(false);
      responseFleets.setMessage("Error while fetching Fleets.");
      kubernetesLogger.error("Error while fetching Fleets.");
    }
    return responseFleets;
  }

  public ResponseFleets getFleetsRoboticsCloud(String roboticsCloudProcessId) {
    ResponseFleets responseFleets = new ResponseFleets();
    try {
      ArrayList<Fleet> fleets = kubernetesRepository
          .getFleetsRoboticsCloud(roboticsCloudProcessId);
      responseFleets.setData(fleets);
      responseFleets.setSuccess(true);
      responseFleets.setMessage("Fleets fetched successfully.");
      kubernetesLogger.info("Fleets fetched successfully.");

    } catch (Exception e) {
      responseFleets.setSuccess(false);
      responseFleets.setMessage("Error while fetching Fleets." + e.getMessage());
    }
    return responseFleets;
  }

  public ResponsePhysicalInstances getPhysicalInstancesRoboticsCloud(String roboticsCloudProcessId) {
    ResponsePhysicalInstances responsePhysicalInstances = new ResponsePhysicalInstances();
    try {
      ArrayList<PhysicalInstanceKubernetes> physicalInstances = kubernetesRepository
          .getPhysicalInstancesRoboticsCloud(roboticsCloudProcessId);
      responsePhysicalInstances.setData(physicalInstances);
      responsePhysicalInstances.setSuccess(true);
      responsePhysicalInstances.setMessage("Physical Instances fetched successfully.");
      kubernetesLogger.info("Got robot status");
    } catch (Exception e) {
      responsePhysicalInstances.setSuccess(false);
      responsePhysicalInstances.setMessage("Error while fetching Physical Instances." + e.getMessage());
      kubernetesLogger.error("Error occured while getting robot status", e);
    }
    return responsePhysicalInstances;

  }

  public Robot getRobot(Organization organization, String teamName, String roboticsCloudName, String fleetName,
      String robotName) {
    Robot robot = new Robot();
    try {
      robot = kubernetesRepository
          .getRobot(organization, teamName, roboticsCloudName, fleetName, robotName);
      kubernetesLogger.info("Got robot status");
      return robot;
    } catch (Exception e) {
      kubernetesLogger.error("Error occured while getting robot", e);
      return robot;
    }

  }

}