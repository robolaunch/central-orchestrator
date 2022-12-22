package org.robolaunch.models;

import java.io.Serializable;
import java.util.List;

public class Workspace implements Serializable {
  private String name;
  private List<Repository> repositories;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Repository> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<Repository> repositories) {
    this.repositories = repositories;
  }

}
