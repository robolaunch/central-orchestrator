package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.exception.ApplicationException;
import org.robolaunch.models.Organization;
import org.robolaunch.models.request.RequestBuildManager;
import org.robolaunch.models.request.RequestLaunchManager;
import org.robolaunch.models.request.RequestRobot;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.repository.abstracts.RobotRepository;

import com.google.gson.Gson;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class RobotService {
  @Inject
  Logger log;

  @Inject
  RobotRepository robotRepository;

  @Inject
  JsonWebToken jwt;

  @LoggerName("robotService")
  Logger robotLogger;

  public PlainResponse makeRobotsPassive(String bufferName, String provider, String region, String superCluster) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      robotRepository.makeRobotsPassive(bufferName, provider, region, superCluster);
      robotLogger.info("Robots made passive");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      robotLogger.error("Error occured while making robots passive", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse makeRobotsActive(String bufferName, String provider, String region, String superCluster) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      robotRepository.makeRobotsActive(bufferName, provider, region, superCluster);
      robotLogger.info("Robots made active");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      robotLogger.error("Error occured while making robots active", e);
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse createBuildManager(RequestBuildManager robotBuildManager) {
    PlainResponse plainResponse = new PlainResponse();
    System.out.println(" out createRobotBuildManager out");

    try {
      String token = jwt.getRawToken();
      if (robotBuildManager.isFederated()) {

        robotRepository.createFederatedRobotBuildManager(robotBuildManager, token);
      } else {
        robotRepository.createRobotBuildManager(robotBuildManager, token);
      }
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Robot build manager created.");
      robotLogger.info("Robot build manager created");
    } catch (Exception e) {
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error occured while creating robot build manager.");
      robotLogger.error("Error occured while creating robot build manager", e);
    }
    return plainResponse;
  }

  public PlainResponse createLaunchManager(RequestLaunchManager robotLaunchManager) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      String token = jwt.getRawToken();
      if (robotLaunchManager.isFederated()) {
        robotRepository.createFederatedRobotLaunchManager(robotLaunchManager, token);
      } else {
        robotRepository.createRobotLaunchManager(robotLaunchManager, token);
      }
      robotLogger.info("Robot launch manager created");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Robot launch manager created.");
    } catch (Exception e) {
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error occured while creating robot launch manager.");
    }
    return plainResponse;
  }

  public PlainResponse createRobot(RequestRobot requestRobot) {
    PlainResponse plainResponse = new PlainResponse();
    Gson gson = new Gson();
    try {
      String token = jwt.getRawToken();
      if (!requestRobot.isFederated()) {
        System.out.println("Robot is not federated: " + gson.toJson(requestRobot));
        System.out.println("------------------------------>");
        robotRepository.createRobot(requestRobot, token);

      } else {
        System.out.println("Robot is federated: " + gson.toJson(requestRobot));
        System.out.println("------------------------------>");
        robotRepository.createFederatedRobot(requestRobot, token);
      }
      robotLogger.info("Robot created");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Robot created.");
    } catch (ApplicationException e) {
      robotLogger.error("Error occured while creating robot: ", e);
      plainResponse.setSuccess(false);
      plainResponse.setMessage("An error occured creating robot. Please try again.");
    } catch (Exception e) {
      robotLogger.error("Error occured while creating robot", e);
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error occured while creating robot.");
    }
    return plainResponse;
  }

  public String hybridRobotScript(String provider, String region, String superCluster, Organization organization,
      String teamId, String bufferName,
      String cloudInstanceName, String physicalInstanceName) {
    try {
      String script = robotRepository.hybridRobotScript(provider, region, superCluster, organization, teamId,
          bufferName,
          cloudInstanceName, physicalInstanceName);
      robotLogger.info("Hybrid robot script created");
      return script;
    } catch (Exception e) {
      robotLogger.error("Error occured while creating hybrid robot script", e);
      return null;
    }
  }

  public String getRobotStatus(String fleetProcessId, String robotName) {
    try {
      String robotStatus = robotRepository.getRobotStatus(fleetProcessId,
          robotName);
      robotLogger.info("Got robot status");
      return robotStatus;
    } catch (Exception e) {
      robotLogger.error("Error occured while getting robot status", e);
      return null;
    }
  }

}
