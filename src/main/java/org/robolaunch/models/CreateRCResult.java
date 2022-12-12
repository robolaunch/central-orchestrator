package org.robolaunch.models;

public class CreateRCResult {
  private boolean canCreate;
  private String reason;

  public CreateRCResult() {
  }

  public CreateRCResult(boolean canCreate, String reason) {
    this.canCreate = canCreate;
    this.reason = reason;
  }

  public boolean isCanCreate() {
    return canCreate;
  }

  public void setCanCreate(boolean canCreate) {
    this.canCreate = canCreate;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

}
