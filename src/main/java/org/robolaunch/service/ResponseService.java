package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;

import org.robolaunch.model.response.PlainResponse;

@ApplicationScoped
public class ResponseService {

  public PlainResponse plainResponseTemplate(String message, Boolean success) {
    PlainResponse plainResponse = new PlainResponse();
    plainResponse.setMessage(message);
    plainResponse.setSuccess(success);
    return plainResponse;
  }

}
