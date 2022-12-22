package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.robolaunch.models.Response;
import org.robolaunch.models.request.RobotBuildManager;
import org.robolaunch.models.request.RobotDevSuite;
import org.robolaunch.models.request.RobotLaunchManager;
import org.robolaunch.models.response.PlainResponse;
import org.robolaunch.repository.abstracts.RobotRepository;

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

  public Response makeRobotsPassive(String bufferName) {
    try {
      robotRepository.makeRobotsPassive(bufferName);
      robotLogger.info("Robots made passive");
      return new Response(true, "Robots are now passive");
    } catch (Exception e) {
      robotLogger.error("Error occured while making robots passive", e);
      return new Response(false, "Error occured while making robots passive");
    }
  }

  public Response makeRobotsActive(String bufferName) {
    try {
      robotRepository.makeRobotsActive(bufferName);
      robotLogger.info("Robots made active");

      return new Response(true, "Robots are now active");
    } catch (Exception e) {
      robotLogger.error("Error occured while making robots active", e);
      return new Response(false, "Error occured while making robots active");
    }
  }

  public PlainResponse createRobotBuildManager(RobotBuildManager robotBuildManager, String bufferName) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      String token = jwt.getRawToken();
      robotRepository.createRobotBuildManager(robotBuildManager, bufferName, token);
      robotLogger.info("Robot build manager created");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Robot build manager created.");
    } catch (Exception e) {
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error occured while creating robot build manager.");
    }
    return plainResponse;
  }

  public PlainResponse createRobotLaunchManager(RobotLaunchManager robotLaunchManager, String bufferName) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      String token = jwt.getRawToken();
      robotRepository.createRobotLaunchManager(robotLaunchManager, bufferName, token);
      robotLogger.info("Robot launch manager created");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Robot launch manager created.");
    } catch (Exception e) {
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error occured while creating robot launch manager.");
    }
    return plainResponse;
  }

  public PlainResponse createRobotDevSuite(RobotDevSuite robotDevSuite, String bufferName) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      String token = jwt.getRawToken();
      robotRepository.createRobotDevelopmentSuite(robotDevSuite, bufferName, token);
      robotLogger.info("Robot development suite created");
      plainResponse.setSuccess(true);
      plainResponse.setMessage("Robot development suite created.");
    } catch (Exception e) {
      plainResponse.setSuccess(false);
      plainResponse.setMessage("Error occured while creating robot development suite.");
    }
    return plainResponse;
  }

}
