package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.robolaunch.models.Response;
import org.robolaunch.repository.abstracts.AmazonRepository;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class AmazonService {

  @Inject
  Logger log;

  @Inject
  AmazonRepository amazonRepository;

  @LoggerName("amazonService")
  Logger amazonLogger;

  public Response stopInstance(String nodeName, String region) {
    try {
      amazonRepository.stopInstance(nodeName, region);
      amazonLogger.info("Instance stopped");
      return new Response(true, "Instance stop!.");

    } catch (Exception e) {
      amazonLogger.error("Instance could not be stopped");
      return new Response(false, "Instance could not be stopped");
    }
  }

  public Response startInstance(String nodeName, String region) {
    try {
      amazonRepository.startInstance(nodeName, region);
      amazonLogger.info("Instance started");
      return new Response(true, "Instance started");
    } catch (Exception e) {
      amazonLogger.error("Instance could not be started");
      return new Response(false, "Instance could not be started");
    }
  }

  public Boolean isInstanceStopped(String nodeName, String region) {
    try {
      Boolean isStopped = amazonRepository.isInstanceStopped(nodeName, region);
      return isStopped;
    } catch (Exception e) {
      amazonLogger.error("Instance could not be started");
      return false;
    }
  }
}
