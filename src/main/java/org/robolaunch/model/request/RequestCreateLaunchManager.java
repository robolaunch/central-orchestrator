package org.robolaunch.model.request;

import java.io.Serializable;
import java.util.ArrayList;

public class RequestCreateLaunchManager implements Serializable {
   private String apiVersion;
   private String kind;
   private RequestCloudRobotMetadata metadata;
   private ArrayList<RequestLaunchManagerSpecLaunch> launch;
   private Boolean display;
   private String roboticsCloudName;

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

   public Boolean isDisplay() {
      return display;
   }

   public void setDisplay(Boolean display) {
      this.display = display;
   }

   public String getRoboticsCloudName() {
      return roboticsCloudName;
   }

   public void setRoboticsCloudName(String roboticsCloudName) {
      this.roboticsCloudName = roboticsCloudName;
   }

}
