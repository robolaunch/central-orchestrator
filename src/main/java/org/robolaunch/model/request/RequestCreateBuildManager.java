package org.robolaunch.model.request;

import java.io.Serializable;
import java.util.ArrayList;

public class RequestCreateBuildManager implements Serializable {
   private String apiVersion;
   private String kind;
   private RequestCloudRobotMetadata metadata;
   private ArrayList<BuildManagerStep> steps;

   public RequestCreateBuildManager() {
   }

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

   public ArrayList<BuildManagerStep> getSteps() {
      return steps;
   }

   public void setSteps(ArrayList<BuildManagerStep> steps) {
      this.steps = steps;
   }

}
