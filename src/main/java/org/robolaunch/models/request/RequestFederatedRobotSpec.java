package org.robolaunch.models.request;

import java.io.Serializable;
import java.util.ArrayList;

public class RequestFederatedRobotSpec implements Serializable {
   private RequestCloudRobotMetadata metadata;
   private RequestFederatedRobotSpecTemplateSpec spec;
   private ArrayList<String> clusters;

   public ArrayList<String> getClusters() {
      return clusters;
   }

   public void setClusters(ArrayList<String> clusters) {
      this.clusters = clusters;
   }

   public RequestCloudRobotMetadata getMetadata() {
      return metadata;
   }

   public void setMetadata(RequestCloudRobotMetadata metadata) {
      this.metadata = metadata;
   }

   public RequestFederatedRobotSpecTemplateSpec getSpec() {
      return spec;
   }

   public void setSpec(RequestFederatedRobotSpecTemplateSpec spec) {
      this.spec = spec;
   }

}
