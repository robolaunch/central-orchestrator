package org.robolaunch.model.account;

public class LoginRefreshTokenOrganization extends LoginRefreshToken {
  private String organization;

  public LoginRefreshTokenOrganization() {
  }

  public LoginRefreshTokenOrganization(String refreshToken, String organization) {
    super(refreshToken);
    this.organization = organization;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

}
