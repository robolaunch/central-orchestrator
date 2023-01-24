package org.robolaunch.model.ipa;

import java.io.Serializable;

public class IPAGroup implements Serializable {
    private String name;

    public IPAGroup() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IPAGroup(String name) {
        this.name = name;
    }
}
