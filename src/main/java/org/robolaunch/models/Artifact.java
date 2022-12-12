package org.robolaunch.models;

import java.io.Serializable;

public class Artifact implements Serializable {
    private String name;
    private String clusterName;

    public Artifact() {
    }

    public Artifact(String name, String clusterName) {
        this.name = name;
        this.clusterName = clusterName;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

}
