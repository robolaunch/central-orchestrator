package org.robolaunch.models;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DepartmentAdd implements Serializable {
  @JsonProperty("name")
  private String name;

  @JsonProperty("isAdmin")
  private Boolean isAdmin;

  public DepartmentAdd() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(Boolean isAdmin) {
    this.isAdmin = isAdmin;
  }

  @Override
  public String toString() {
    return "DepartmentAdd [isAdmin=" + isAdmin + ", name=" + name + "]";
  }

}
