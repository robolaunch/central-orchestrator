package org.robolaunch.models;

import java.io.Serializable;

public class LoginRequestOrganization implements Serializable {
  private String username;
  private String password;
  private String organization;

  public LoginRequestOrganization() {
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

}
