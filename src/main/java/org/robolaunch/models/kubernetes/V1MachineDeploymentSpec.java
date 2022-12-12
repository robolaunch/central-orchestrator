package org.robolaunch.models.kubernetes;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class V1MachineDeploymentSpec {
  public static final String SERIALIZED_NAME_CRON_SPEC = "cronSpec";
  @SerializedName(SERIALIZED_NAME_CRON_SPEC)
  private String cronSpec;

  public static final String SERIALIZED_NAME_IMAGE = "image";
  @SerializedName(SERIALIZED_NAME_IMAGE)
  private String image;

  public static final String SERIALIZED_NAME_REPLICAS = "replicas";
  @SerializedName(SERIALIZED_NAME_REPLICAS)
  private Integer replicas;

  public V1MachineDeploymentSpec cronSpec(String cronSpec) {
    this.cronSpec = cronSpec;
    return this;
  }

  public void setCronSpec(String cronSpec) {
    this.cronSpec = cronSpec;
  }

  public V1MachineDeploymentSpec image(String image) {
    this.image = image;
    return this;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public V1MachineDeploymentSpec replicas(Integer replicas) {
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
    V1MachineDeploymentSpec v1MachineDeploymentSpec = (V1MachineDeploymentSpec) o;
    return Objects.equals(this.cronSpec, v1MachineDeploymentSpec.cronSpec) &&
        Objects.equals(this.image, v1MachineDeploymentSpec.image) &&
        Objects.equals(this.replicas, v1MachineDeploymentSpec.replicas);
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
