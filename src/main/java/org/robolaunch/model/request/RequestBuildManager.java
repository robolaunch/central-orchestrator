package org.robolaunch.model.request;

import java.io.Serializable;

import org.robolaunch.model.account.Organization;

public class RequestBuildManager implements Serializable {
   private RequestCreateBuildManager buildManager;
   private String targetRobot;
   private Organization organization;
   private String teamId;
   private String roboticsCloudName;
   private Boolean federated;
   private RequestCreateFederatedBuildManager federatedBuildManager;

   public RequestBuildManager() {
   }

   public RequestCreateBuildManager getBuildManager() {
      return buildManager;
   }

   public void setBuildManager(RequestCreateBuildManager buildManager) {
      this.buildManager = buildManager;
   }

   public String getTargetRobot() {
      return targetRobot;
   }

   public void setTargetRobot(String targetRobot) {
      this.targetRobot = targetRobot;
   }

   public Organization getOrganization() {
      return organization;
   }

   public void setOrganization(Organization organization) {
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

   public RequestCreateFederatedBuildManager getFederatedBuildManager() {
      return federatedBuildManager;
   }

   public void setFederatedBuildManager(RequestCreateFederatedBuildManager federatedBuildManager) {
      this.federatedBuildManager = federatedBuildManager;
   }

   public String getRoboticsCloudName() {
      return roboticsCloudName;
   }

   public void setRoboticsCloudName(String roboticsCloudName) {
      this.roboticsCloudName = roboticsCloudName;
   }

}
