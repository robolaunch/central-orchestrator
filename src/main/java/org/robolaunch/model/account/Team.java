package org.robolaunch.model.account;

import java.io.Serializable;
import java.util.List;

public class Team implements Serializable {
  private String id;
  private String name;
  private List<UserGroupMember> users;

  public Team() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<UserGroupMember> getUsers() {
    return users;
  }

  public void setUsers(List<UserGroupMember> users) {
    this.users = users;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "Team [id=" + id + ", name=" + name + ", users=" + users + "]";
  }

}
