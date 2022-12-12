package org.robolaunch.models.kubernetes;

import com.google.gson.annotations.SerializedName;

public class V1VirtualClusterStatus {
  public static final String SERIALIZED_NAME_PHASE = "phase";
  @SerializedName(SERIALIZED_NAME_PHASE)
  private String phase;

  public V1VirtualClusterStatus status(String phase) {
    this.phase = phase;
    return this;
  }

  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

}
