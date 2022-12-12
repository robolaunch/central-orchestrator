package org.robolaunch.models;

import java.io.Serializable;
import java.net.HttpCookie;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LoginResponseWithIPA implements Serializable {

  private List<HttpCookie> cookies;
  private CompletableFuture<LoginResponse> loginResponse;

  public LoginResponseWithIPA() {
  }

  public List<HttpCookie> getCookies() {
    return cookies;
  }

  public void setCookies(List<HttpCookie> cookies) {
    this.cookies = cookies;
  }

  public CompletableFuture<LoginResponse> getLoginResponse() {
    return loginResponse;
  }

  public void setLoginResponse(CompletableFuture<LoginResponse> loginResponse) {
    this.loginResponse = loginResponse;
  }

}
