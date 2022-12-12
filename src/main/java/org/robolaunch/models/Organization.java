package org.robolaunch.models;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class Organization implements Serializable {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String name;

    private Boolean enterprise;

    public Organization() {
    }

    public Organization(String name, Boolean enterprise) {
        this.name = name;
        this.enterprise = enterprise;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isEnterprise() {
        return enterprise;
    }

    public void setEnterprise(Boolean enterprise) {
        this.enterprise = enterprise;
    }

}
