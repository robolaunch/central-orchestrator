package org.robolaunch.models;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Department implements Serializable {

  @JsonProperty("name")
  private String name;

  @JsonProperty("users")
  private List<GroupMember> users;

  @JsonProperty("id")
  private String id;

  public Department() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<GroupMember> getUsers() {
    return users;
  }

  public void setUsers(List<GroupMember> users) {
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
    return "Department [id=" + id + ", name=" + name + ", users=" + users + "]";
  }

}
