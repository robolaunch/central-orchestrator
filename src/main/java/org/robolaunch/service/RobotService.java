package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.robolaunch.models.Response;
import org.robolaunch.repository.abstracts.RobotRepository;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class RobotService {
  @Inject
  Logger log;

  @Inject
  RobotRepository robotRepository;

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

}
