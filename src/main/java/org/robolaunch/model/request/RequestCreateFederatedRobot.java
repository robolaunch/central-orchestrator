package org.robolaunch.model.request;

import java.io.Serializable;

public class RequestCreateFederatedRobot implements Serializable {
   private String apiVersion;
   private String kind;
   private RequestFederatedRobotMetadata metadata;
   private RequestFederatedRobotSpec spec;

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

   public RequestFederatedRobotMetadata getMetadata() {
      return metadata;
   }

   public void setMetadata(RequestFederatedRobotMetadata metadata) {
      this.metadata = metadata;
   }

   public RequestFederatedRobotSpec getSpec() {
      return spec;
   }

   public void setSpec(RequestFederatedRobotSpec spec) {
      this.spec = spec;
   }

}
