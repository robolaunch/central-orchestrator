package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.robolaunch.models.Result;
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

  public Result alreadyExists() {
    errorLogger.error("Already exists.");
    return new Result("Already exists.", false);
  }

  public Result notAuthorized() {
    errorLogger.error("Authorization error.");
    return new Result("You are not authorized.", false);
  }

  public Result notFound() {
    errorLogger.error("Not found.");
    return new Result("Resource not found.", false);
  }

  public Integer test(Integer count) {
    return count + 1;
  }

}
