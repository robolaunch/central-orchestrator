package org.robolaunch.models;

import java.io.Serializable;

import javax.validation.constraints.Email;

public class User implements Serializable {

  private String username;

  @Email
  private String email;
  private String firstName;
  private String lastName;
  private String invitedEmail;
  private String invitedBy;
  private String invitedStatus;
  private String invitedOrganization;

  public User() {
  }

  public User(String username, String email, String firstName, String lastName, String invitedEmail, String invitedBy,
      String invitedStatus, String invitedOrganization) {
    this.username = username;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.invitedEmail = invitedEmail;
    this.invitedBy = invitedBy;
    this.invitedStatus = invitedStatus;
    this.invitedOrganization = invitedOrganization;
  }

  public String getInvitedBy() {
    return invitedBy;
  }

  public void setInvitedBy(String invitedBy) {
    this.invitedBy = invitedBy;
  }

  public String getInvitedStatus() {
    return invitedStatus;
  }

  public void setInvitedStatus(String invitedStatus) {
    this.invitedStatus = invitedStatus;
  }

  public String getInvitedOrganization() {
    return invitedOrganization;
  }

  public void setInvitedOrganization(String invitedOrganization) {
    this.invitedOrganization = invitedOrganization;
  }

  public String getInvitedEmail() {
    return invitedEmail;
  }

  public void setInvitedEmail(String invitedEmail) {
    this.invitedEmail = invitedEmail;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
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

}
