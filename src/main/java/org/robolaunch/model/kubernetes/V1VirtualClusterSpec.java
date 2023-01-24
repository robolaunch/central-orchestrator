package org.robolaunch.model.kubernetes;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModelProperty;

public class V1VirtualClusterSpec {
  public static final String SERIALIZED_NAME_CRON_SPEC = "cronSpec";
  @SerializedName(SERIALIZED_NAME_CRON_SPEC)
  private String cronSpec;

  public static final String SERIALIZED_NAME_IMAGE = "image";
  @SerializedName(SERIALIZED_NAME_IMAGE)
  private String image;

  public static final String SERIALIZED_NAME_REPLICAS = "replicas";
  @SerializedName(SERIALIZED_NAME_REPLICAS)
  private Integer replicas;

  public static final String SERIALIZED_NAME_CLUSTER_VERSION_NAME = "clusterVersionName";
  @SerializedName(SERIALIZED_NAME_CLUSTER_VERSION_NAME)
  private String clusterVersionName;

  public static final String SERIALIZED_NAME_STATUS = "status";
  @SerializedName(SERIALIZED_NAME_STATUS)
  private V1VirtualClusterStatus status;

  public V1VirtualClusterSpec status(V1VirtualClusterStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get spec
   * 
   * @return spec
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public V1VirtualClusterStatus getStatus() {
    return status;
  }

  public void setstatus(V1VirtualClusterStatus status) {
    this.status = status;
  }

  public V1VirtualClusterSpec cronSpec(String cronSpec) {
    this.cronSpec = cronSpec;
    return this;
  }

  public void setCronSpec(String cronSpec) {
    this.cronSpec = cronSpec;
  }

  public V1VirtualClusterSpec clusterVersionName(String clusterVersionName) {
    this.clusterVersionName = clusterVersionName;
    return this;
  }

  public String getClusterVersionName() {
    return clusterVersionName;
  }

  public void getClusterVersionName(String clusterVersionName) {
    this.clusterVersionName = clusterVersionName;
  }

  public void setClusterVersionName(String clusterVersionName) {
    this.clusterVersionName = clusterVersionName;
  }

  public V1VirtualClusterSpec image(String image) {

    this.image = image;
    return this;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public V1VirtualClusterSpec replicas(Integer replicas) {

    this.replicas = replicas;
    return this;
  }

  public Integer getReplicas() {
    return replicas;
  }

  public void setReplicas(Integer replicas) {
    this.replicas = replicas;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1VirtualClusterSpec v1VirtualClusterSpec = (V1VirtualClusterSpec) o;
    return Objects.equals(this.cronSpec, v1VirtualClusterSpec.cronSpec) &&
        Objects.equals(this.image, v1VirtualClusterSpec.image) &&
        Objects.equals(this.replicas, v1VirtualClusterSpec.replicas);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cronSpec, image, replicas);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1CloudRobotSpec {\n");
    sb.append("    cronSpec: ").append(toIndentedString(cronSpec)).append("\n");
    sb.append("    image: ").append(toIndentedString(image)).append("\n");
    sb.append("    replicas: ").append(toIndentedString(replicas)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
