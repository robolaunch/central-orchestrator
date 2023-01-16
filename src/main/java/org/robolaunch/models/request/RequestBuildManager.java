package org.robolaunch.models.request;

import java.io.Serializable;

import org.robolaunch.models.Organization;

public class RequestBuildManager implements Serializable {
   private RequestCreateBuildManager buildManager;
   private String robotProcessId;
   private String targetRobot;
   private Organization organization;
   private String teamId;

   public RequestBuildManager() {
   }

   public RequestCreateBuildManager getBuildManager() {
      return buildManager;
   }

   public void setBuildManager(RequestCreateBuildManager buildManager) {
      this.buildManager = buildManager;
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

}
