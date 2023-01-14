package org.robolaunch.models.request;

public class RequestCreateLaunchManager {
   private String apiVersion;
   private String kind;
   private RequestCloudRobotMetadata metadata;
   private RequestLaunchManagerSpec spec;

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

   public RequestLaunchManagerSpec getSpec() {
      return spec;
   }

   public void setSpec(RequestLaunchManagerSpec spec) {
      this.spec = spec;
   }

}
