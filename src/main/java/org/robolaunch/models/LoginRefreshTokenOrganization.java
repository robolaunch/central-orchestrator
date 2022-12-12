package org.robolaunch.models;

import java.io.Serializable;

public class LoginRefreshTokenOrganization implements Serializable {

  private String refreshToken;
  private String organization;

  public LoginRefreshTokenOrganization() {
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

}
