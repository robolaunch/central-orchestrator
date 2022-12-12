package org.robolaunch.models;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InvitedUser implements Serializable {

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

  public InvitedUser() {
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

  @Override
  public String toString() {
    return "InvitedUser [admin=" + admin + ", email=" + email + ", firstName=" + firstName + ", lastName=" + lastName
        + ", username=" + username + "]";
  }

}
