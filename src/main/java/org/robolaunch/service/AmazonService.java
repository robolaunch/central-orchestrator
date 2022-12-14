package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.robolaunch.models.response.PlainResponse;
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

  public PlainResponse stopInstance(String nodeName, String provider, String region, String superCluster) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      amazonRepository.stopInstance(nodeName, provider, region, superCluster);
      amazonLogger.info("Instance stopped");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      amazonLogger.error("Instance could not be stopped");
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public PlainResponse startInstance(String nodeName, String provider, String region, String superCluster) {
    PlainResponse plainResponse = new PlainResponse();
    try {
      amazonRepository.startInstance(nodeName, provider, region, superCluster);
      amazonLogger.info("Instance started");
      plainResponse.setSuccess(true);
    } catch (Exception e) {
      amazonLogger.error("Instance could not be started");
      plainResponse.setSuccess(false);
    }
    return plainResponse;
  }

  public Boolean isInstanceStopped(String nodeName, String provider, String region, String superCluster) {
    try {
      Boolean isStopped = amazonRepository.isInstanceStopped(nodeName, provider, region, superCluster);
      return isStopped;
    } catch (Exception e) {
      amazonLogger.error("Instance could not be started");
      return false;
    }
  }
}
