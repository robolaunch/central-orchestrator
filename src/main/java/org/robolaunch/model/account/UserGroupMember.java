package org.robolaunch.model.account;

public class UserGroupMember extends User {
  private Boolean admin;

  public UserGroupMember() {
  }

  public UserGroupMember(String username, String email, String firstName, String lastName, Boolean admin) {
    super(username, email, firstName, lastName);
    this.admin = admin;
  }

  public Boolean isAdmin() {
    return admin;
  }

  public void setAdmin(Boolean admin) {
    this.admin = admin;
  }

}
