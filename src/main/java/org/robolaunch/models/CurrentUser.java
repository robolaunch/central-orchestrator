package org.robolaunch.models;

import java.io.Serializable;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CurrentUser implements Serializable {

  @JsonProperty("username")
  private String username;

  @JsonProperty("firstName")
  private String firstName;

  @JsonProperty("lastName")
  private String lastName;

  @JsonProperty("email")
  private String email;

  @JsonProperty("admin")
  private Boolean admin;

  private ArrayList<RegisteredDepartment> departments;

  public CurrentUser() {
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Boolean isAdmin() {
    return admin;
  }

  public void setAdmin(Boolean admin) {
    this.admin = admin;
  }

  public ArrayList<RegisteredDepartment> getDepartments() {
    return departments;
  }

  public void setDepartments(ArrayList<RegisteredDepartment> departments) {
    this.departments = departments;
  }

  @Override
  public String toString() {
    return "CurrentUser [admin=" + admin + ", departments=" + departments + ", email=" + email + ", firstName="
        + firstName + ", lastName=" + lastName + ", username=" + username + "]";
  }

  public Boolean getAdmin() {
    return admin;
  }

}
