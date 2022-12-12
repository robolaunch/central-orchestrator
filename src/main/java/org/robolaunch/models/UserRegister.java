package org.robolaunch.models;

import java.io.Serializable;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class UserRegister implements Serializable {

  @NotBlank
  @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
  @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "Username must be alphanumeric with no spaces")
  @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
  private String username;

  @NotBlank
  @Size(min = 3, max = 32, message = "First name must be between 3 and 32 characters")
  private String firstName;

  @NotBlank
  @Size(min = 3, max = 32, message = "Last name must be between 3 and 32 characters")
  private String lastName;

  @NotBlank
  @Email(message = "Email should be valid")
  private String email;

  public UserRegister() {
  }

  public UserRegister(String username, String firstName, String lastName, String email) {
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
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

}
