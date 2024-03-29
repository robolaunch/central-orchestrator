package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCreateFederatedLaunchManager implements Serializable {
   private String apiVersion;
   private String kind;
   private RequestFederatedRobotMetadata metadata;

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

}
