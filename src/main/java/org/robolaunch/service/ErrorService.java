package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.robolaunch.models.response.PlainResponse;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class ErrorService {
  @Inject
  Logger log;

  @LoggerName("organizationService")
  Logger errorLogger;

  public PlainResponse plainResponseTemplate(String message, Boolean success) {
    PlainResponse plainResponse = new PlainResponse();
    plainResponse.setMessage(message);
    plainResponse.setSuccess(success);
    return plainResponse;
  }

}
