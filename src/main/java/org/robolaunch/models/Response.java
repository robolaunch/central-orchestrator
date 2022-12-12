package org.robolaunch.models;

import java.io.Serializable;

public class Response implements Serializable {

  private Boolean success;
  private String resourceId;

  public Response() {
  }

  public Response(Boolean success, String resourceId) {
    this.success = success;
    this.resourceId = resourceId;
  }

  public static Response success(String payload) {
    return new Response(true, payload);
  }

  public static Response error(String payload) {
    return new Response(false, payload);
  }

  public Boolean isSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

}