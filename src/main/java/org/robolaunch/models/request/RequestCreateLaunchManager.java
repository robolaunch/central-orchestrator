package org.robolaunch.models.request;

import java.io.Serializable;
import java.util.ArrayList;

public class RequestCreateLaunchManager implements Serializable {
   private String apiVersion;
   private String kind;
   private RequestCloudRobotMetadata metadata;
   private ArrayList<RequestLaunchManagerSpecLaunch> launch;

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

   public ArrayList<RequestLaunchManagerSpecLaunch> getLaunch() {
      return launch;
   }

   public void setLaunch(ArrayList<RequestLaunchManagerSpecLaunch> launch) {
      this.launch = launch;
   }

}
