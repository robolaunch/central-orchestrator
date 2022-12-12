package org.robolaunch.models;

public class Result {

  private String message;
  private Boolean success;

  public Result() {

  }

  public Result(String message) {
    this.success = true;
    this.message = message;
  }

  public Result(String message, Boolean success) {
    this.message = message;
    this.success = success;
  }

  public String getMessage() {
    return message;
  }

  public Boolean isSuccess() {
    return success;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

}