package org.robolaunch.models;

import java.io.Serializable;

public class CloudInstance implements Serializable {

  private String processId;

  private String bufferName;

  private String name;

  private String status;

  private Number diskSize;

  private String instanceType;

  private String region;

  public CloudInstance() {
  }

  public CloudInstance(String name, String status, Number diskSize, String instanceType, String region,
      String processId) {
    this.name = name;
    this.status = status;
    this.diskSize = diskSize;
    this.instanceType = instanceType;
    this.region = region;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Number getDiskSize() {
    return diskSize;
  }

  public void setDiskSize(Number diskSize) {
    this.diskSize = diskSize;
  }

  public String getInstanceType() {
    return instanceType;
  }

  public void setInstanceType(String instanceType) {
    this.instanceType = instanceType;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getProcessId() {
    return processId;
  }

  public void setProcessId(String processId) {
    this.processId = processId;
  }

  @Override
  public String toString() {
    return "CloudInstance [processId=" + processId + ", name=" + name + ", status=" + status + ", diskSize=" + diskSize
        + ", instanceType=" + instanceType + ", region=" + region + "]";
  }

  public String getBufferName() {
    return bufferName;
  }

  public void setBufferName(String bufferName) {
    this.bufferName = bufferName;
  }

}
