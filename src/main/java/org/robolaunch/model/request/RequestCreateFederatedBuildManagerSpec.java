package org.robolaunch.model.request;

import java.io.Serializable;
import java.util.ArrayList;

public class RequestCreateFederatedBuildManagerSpec implements Serializable {
   private RequestFederatedRobotMetadata metadata;
   private ArrayList<BuildManagerStep> steps;
   private ArrayList<String> clusters;

   public RequestFederatedRobotMetadata getMetadata() {
      return metadata;
   }

   public void setMetadata(RequestFederatedRobotMetadata metadata) {
      this.metadata = metadata;
   }

   public ArrayList<BuildManagerStep> getSteps() {
      return steps;
   }

   public void setSteps(ArrayList<BuildManagerStep> steps) {
      this.steps = steps;
   }

   public ArrayList<String> getClusters() {
      return clusters;
   }

   public void setClusters(ArrayList<String> clusters) {
      this.clusters = clusters;
   }

}
