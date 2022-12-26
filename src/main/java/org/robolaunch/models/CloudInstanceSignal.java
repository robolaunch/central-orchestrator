package org.robolaunch.models;

import java.io.Serializable;

public class CloudInstanceSignal implements Serializable {
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
