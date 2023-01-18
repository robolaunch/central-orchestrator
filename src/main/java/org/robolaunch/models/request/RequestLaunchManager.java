package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestLaunchManager implements Serializable {
   private RequestCreateLaunchManager launchManager;
   private String robotProcessId;
   private String targetRobot;
   private String organization;
   private String teamId;
   private Boolean federated;
   private RequestCreateFederatedLaunchManager federatedLaunchManager;

   public RequestLaunchManager() {
   }

   public RequestCreateLaunchManager getLaunchManager() {
      return launchManager;
   }

   public void setLaunchManager(RequestCreateLaunchManager launchManager) {
      this.launchManager = launchManager;
   }

   public String getRobotProcessId() {
      return robotProcessId;
   }

   public void setRobotProcessId(String robotProcessId) {
      this.robotProcessId = robotProcessId;
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

}
