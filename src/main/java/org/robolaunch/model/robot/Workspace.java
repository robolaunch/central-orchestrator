package org.robolaunch.model.robot;

import java.io.Serializable;
import java.util.List;

public class Workspace implements Serializable {
  private String name;
  private String distro;
  private List<Repository> repositories;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDistro() {
    return distro;
  }

  public void setDistro(String distro) {
    this.distro = distro;
  }

  public List<Repository> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<Repository> repositories) {
    this.repositories = repositories;
  }

}
