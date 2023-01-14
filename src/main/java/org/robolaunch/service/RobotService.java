package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.models.Organization;
import org.robolaunch.models.request.RequestRobot;
import org.robolaunch.models.request.RobotBuildManager;
import org.robolaunch.models.request.RobotDevSuite;
import org.robolaunch.models.request.RobotLaunchManager;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.repository.abstracts.RobotRepository;

import io.kubernetes.client.openapi.ApiException;
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

  public PlainResponse createRobotBuildManager(RobotBuildManager robotBuildManager, String bufferName, String provider,
      String region, String superCluster) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      String token = jwt.getRawToken();
      robotRepository.createRobotBuildManager(robotBuildManager, bufferName, token, provider, region, superCluster);
      robotLogger.info("Robot build manager created");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Robot build manager created.");
    } catch (Exception e) {
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error occured while creating robot build manager.");
    }
    return plainResponse;
  }

  public PlainResponse createRobotLaunchManager(RobotLaunchManager robotLaunchManager, String bufferName,
      String provider, String region, String superCluster) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      String token = jwt.getRawToken();
      robotRepository.createRobotLaunchManager(robotLaunchManager, bufferName, token, provider, region, superCluster);
      robotLogger.info("Robot launch manager created");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Robot launch manager created.");
    } catch (Exception e) {
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error occured while creating robot launch manager.");
    }
    return plainResponse;
  }

  public PlainResponse createRobotDevSuite(RobotDevSuite robotDevSuite, String bufferName, String provider,
      String region, String superCluster) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      String token = jwt.getRawToken();
      robotRepository.createRobotDevelopmentSuite(robotDevSuite, bufferName, token, provider, region, superCluster);
      robotLogger.info("Robot development suite created");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Robot development suite created.");
    } catch (Exception e) {
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error occured while creating robot development suite.");
    }
    return plainResponse;
  }

  public PlainResponse createRobot(RequestRobot requestRobot) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      String token = jwt.getRawToken();
      robotRepository.createRobot(requestRobot, token);
      robotLogger.info("Robot created");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Robot created.");
    } catch (ApiException e) {
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

}
