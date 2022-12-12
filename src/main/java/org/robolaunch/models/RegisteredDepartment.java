package org.robolaunch.models;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisteredDepartment implements Serializable {

  @JsonProperty("name")
  private String name;

  @JsonProperty("admin")
  private Boolean admin;

  public RegisteredDepartment() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean isAdmin() {
    return admin;
  }

  public void setAdmin(Boolean admin) {
    this.admin = admin;
  }

  @Override
  public String toString() {
    return "RegisteredDepartment [admin=" + admin + ", name=" + name + "]";
  }

}
