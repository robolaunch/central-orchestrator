package org.robolaunch.models;

public class CloudInstanceSignal {
  private String operation;

  public CloudInstanceSignal() {
  }

  public CloudInstanceSignal(String operation) {
    this.operation = operation;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

}
