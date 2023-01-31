package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestLaunchManager implements Serializable {
   private RequestCreateLaunchManager launchManager;
   private String targetRobot;
   private String organization;
   private String teamId;
   private String roboticsCloudName;
   private String bufferName;
   private Boolean federated;
   private Boolean vdiEnabled;
   private String fleetName;

   private RequestCreateFederatedLaunchManager federatedLaunchManager;

   public RequestLaunchManager() {
   }

   public RequestCreateLaunchManager getLaunchManager() {
      return launchManager;
   }

   public void setLaunchManager(RequestCreateLaunchManager launchManager) {
      this.launchManager = launchManager;
   }

   public String getTargetRobot() {
      return targetRobot;
   }

   public void setTargetRobot(String targetRobot) {
      this.targetRobot = targetRobot;
   }

   public String getOrganization() {
      return organization;
   }

   public void setOrganization(String organization) {
      this.organization = organization;
   }

   public String getTeamId() {
      return teamId;
   }

   public void setTeamId(String teamId) {
      this.teamId = teamId;
   }

   public Boolean isFederated() {
      return federated;
   }

   public void setFederated(Boolean federated) {
      this.federated = federated;
   }

   public RequestCreateFederatedLaunchManager getFederatedLaunchManager() {
      return federatedLaunchManager;
   }

   public void setFederatedLaunchManager(RequestCreateFederatedLaunchManager federatedLaunchManager) {
      this.federatedLaunchManager = federatedLaunchManager;
   }

   public String getRoboticsCloudName() {
      return roboticsCloudName;
   }

   public void setRoboticsCloudName(String roboticsCloudName) {
      this.roboticsCloudName = roboticsCloudName;
   }

   public Boolean isVdiEnabled() {
      return vdiEnabled;
   }

   public void setVdiEnabled(Boolean vdiEnabled) {
      this.vdiEnabled = vdiEnabled;
   }

   public String getBufferName() {
      return bufferName;
   }

   public void setBufferName(String bufferName) {
      this.bufferName = bufferName;
   }

   public String getFleetName() {
      return fleetName;
   }

   public void setFleetName(String fleetName) {
      this.fleetName = fleetName;
   }

}
