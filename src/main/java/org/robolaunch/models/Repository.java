package org.robolaunch.models;

import java.io.Serializable;

public class Repository implements Serializable {
  private String name;
  private String url;
  private String branch;

  public Repository() {
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
