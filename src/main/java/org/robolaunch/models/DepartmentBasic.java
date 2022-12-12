package org.robolaunch.models;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class DepartmentBasic implements Serializable {

  @NotBlank
  @Pattern(regexp = "^[a-zA-Z0-9]+$")
  private String name;

  public DepartmentBasic() {
  }

  public DepartmentBasic(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
