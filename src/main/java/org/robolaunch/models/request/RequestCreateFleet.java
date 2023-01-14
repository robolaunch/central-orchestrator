package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCreateFleet implements Serializable {
   private String apiVersion;
   private String kind;
   private RequestCloudRobotMetadata metadata;
   private RequestFleetSpec spec;

   public String getApiVersion() {
      return apiVersion;
   }

   public void setApiVersion(String apiVersion) {
      this.apiVersion = apiVersion;
   }

   public String getKind() {
      return kind;
   }

   public void setKind(String kind) {
      this.kind = kind;
   }

   public RequestCloudRobotMetadata getMetadata() {
      return metadata;
   }

   public void setMetadata(RequestCloudRobotMetadata metadata) {
      this.metadata = metadata;
   }

   public RequestFleetSpec getSpec() {
      return spec;
   }

   public void setSpec(RequestFleetSpec spec) {
      this.spec = spec;
   }

}