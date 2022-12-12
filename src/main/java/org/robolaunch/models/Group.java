package org.robolaunch.models;

import java.io.Serializable;

public class Group implements Serializable {
    private String name;

    public Group() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Group(String name) {
        this.name = name;
    }
}
