package org.robolaunch.model.account;

public class LoginRequestOrganization extends LoginRequest {
  private String organization;

  public LoginRequestOrganization() {
  }

  public LoginRequestOrganization(String username, String password, String organization) {
    super(username, password);
    this.organization = organization;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

}
