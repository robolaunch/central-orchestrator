package org.robolaunch.model.account;

import java.io.Serializable;

public class Organization implements Serializable {
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
