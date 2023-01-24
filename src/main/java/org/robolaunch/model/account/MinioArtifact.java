package org.robolaunch.model.account;

import java.io.Serializable;

public class MinioArtifact implements Serializable {
    private String name;
    private String bucketName;

    public MinioArtifact() {
    }

    public MinioArtifact(String name, String bucketName) {
        this.name = name;
        this.bucketName = bucketName;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

}
