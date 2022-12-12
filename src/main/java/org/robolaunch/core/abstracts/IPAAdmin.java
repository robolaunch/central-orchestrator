package org.robolaunch.core.abstracts;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;

public interface IPAAdmin {
  String getUsername();

  void setUsername(String username);

  String getPassword();

  void setPassword(String password);

  List<HttpCookie> login() throws IOException, InternalError;

  void changePassword(String newPassword) throws IOException, InternalError;
}
