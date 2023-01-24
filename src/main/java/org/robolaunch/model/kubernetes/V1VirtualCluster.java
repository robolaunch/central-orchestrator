package org.robolaunch.model.kubernetes;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.swagger.annotations.ApiModelProperty;

public class V1VirtualCluster implements KubernetesObject {

  public static final String SERIALIZED_NAME_API_VERSION = "apiVersion";
  @SerializedName(SERIALIZED_NAME_API_VERSION)
  private String apiVersion;

  public static final String SERIALIZED_NAME_KIND = "kind";
  @SerializedName(SERIALIZED_NAME_KIND)
  private String kind;

  public static final String SERIALIZED_NAME_METADATA = "metadata";
  @SerializedName(SERIALIZED_NAME_METADATA)
  private V1ObjectMeta metadata = null;

  public static final String SERIALIZED_NAME_SPEC = "spec";
  @SerializedName(SERIALIZED_NAME_SPEC)
  private V1VirtualClusterSpec spec;

  public static final String SERIALIZED_NAME_STATUS = "status";
  @SerializedName(SERIALIZED_NAME_STATUS)
  private V1VirtualClusterStatus status;

  public V1VirtualCluster apiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
    return this;
  }

  @Override
  public String getApiVersion() {
    return apiVersion;
  }

  @Override
  public String getKind() {
    return kind;
  }

  @Override
  public V1ObjectMeta getMetadata() {
    return metadata;
  }

  public void setMetadata(V1ObjectMeta metadata) {
    this.metadata = metadata;
  }

  public V1VirtualCluster status(V1VirtualClusterStatus status) {
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

  public void setStatus(V1VirtualClusterStatus status) {
    this.status = status;
  }

  public V1VirtualCluster spec(V1VirtualClusterSpec spec) {
    this.spec = spec;
    return this;
  }

  /**
   * Get spec
   * 
   * @return spec
   **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public V1VirtualClusterSpec getSpec() {
    return spec;
  }

  public void setSpec(V1VirtualClusterSpec spec) {
    this.spec = spec;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1VirtualCluster v1VirtualCluster = (V1VirtualCluster) o;
    return Objects.equals(this.apiVersion, v1VirtualCluster.apiVersion) &&
        Objects.equals(this.kind, v1VirtualCluster.kind) &&
        Objects.equals(this.metadata, v1VirtualCluster.metadata) &&
        Objects.equals(this.spec, v1VirtualCluster.spec);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiVersion, kind, metadata, spec);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1CloudRobot {\n");
    sb.append("    apiVersion: ").append(toIndentedString(apiVersion)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    spec: ").append(toIndentedString(spec)).append("\n");
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
