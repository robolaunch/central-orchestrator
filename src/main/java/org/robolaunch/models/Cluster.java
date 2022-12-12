package org.robolaunch.models;

import java.io.Serializable;

public class Cluster implements Serializable {
    private String name;
    private int masterNodeNumber;
    private int workerNodeNumber;
    private int storage;
    private String token;

    public Cluster() {
    }

    public Cluster(String name, String token, int masterNodeNumber, int workerNodeNumber, int storage) {
        this.name = name;
        this.token = token;
        this.workerNodeNumber = workerNodeNumber;
        this.masterNodeNumber = masterNodeNumber;
        this.storage = storage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMasterNodeNumber() {
        return masterNodeNumber;
    }

    public void setMasterNodeNumber(int masterNodeNumber) {
        this.masterNodeNumber = masterNodeNumber;
    }

    public int getWorkerNodeNumber() {
        return workerNodeNumber;
    }

    public void setWorkerNodeNumber(int workerNodeNumber) {
        this.workerNodeNumber = workerNodeNumber;
    }

    public int getStorage() {
        return storage;
    }

    public void setStorage(int storage) {
        this.storage = storage;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
