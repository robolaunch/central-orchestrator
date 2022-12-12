package org.robolaunch.models;

import java.io.Serializable;
import java.util.List;

public class Workspace implements Serializable {
  private String name;
  private List<Repository> repositories;
  private String build;

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

  public String getBuild() {
    return build;
  }

  public void setBuild(String build) {
    this.build = build;
  }

}
