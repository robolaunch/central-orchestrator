package org.robolaunch.models.request;

public class RequestCreateBuildManager {
   private String apiVersion;
   private String kind;
   private RequestCloudRobotMetadata metadata;
   private RequestBuildManagerSpec spec;

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

   public RequestBuildManagerSpec getSpec() {
      return spec;
   }

   public void setSpec(RequestBuildManagerSpec spec) {
      this.spec = spec;
   }

}
