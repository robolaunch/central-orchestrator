package org.robolaunch.service;

import java.util.ArrayList;

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
      kubernetesLogger.error("Error while getting cloud instances from kubernetes buffer", e);
    }
    return 0;
  }

  public Integer getCurrentBufferCountOfType(String instanceType, String provider, String region, String superCluster) {
    try {
      // Get buffering count
      Integer bufferingCount = kubernetesRepository.getCurrentBufferedCountOfType(instanceType, provider, region,
          superCluster);

      // Get buffered count
      Integer bufferedCount = kubernetesRepository.getCurrentBufferingCountOfType(instanceType, provider, region,
          superCluster);

      return bufferingCount + bufferedCount;
    } catch (Exception e) {
      kubernetesLogger.error("Error while getting cloud instances from kubernetes buffer", e);
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
    System.out.println("enters types.");
    ArrayList<String> types = storageService.getSuperClusterBufferTypes(provider, region, superCluster);
    System.out.println("types -> " + types);
    try {
      for (String type : types) {
        System.out.println("type -> " + type);
        Integer desiredBufferCount = getDesiredBufferCountOfType(type, provider, region, superCluster);
        System.out.println("desiredBufferCount -> " + desiredBufferCount);
        Integer currentBufferCount = getCurrentBufferCountOfType(type, provider, region, superCluster);
        System.out.println("currentBufferCount -> " + currentBufferCount);
        if (desiredBufferCount > currentBufferCount) {
          return type;
        }
      }
      return "";
    } catch (Exception e) {
      System.out.println("Error while checking if type needs buffer: " + e.getMessage());
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
}
