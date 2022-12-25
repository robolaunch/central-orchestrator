package org.robolaunch.models.request;

import java.io.Serializable;
import java.util.List;

import org.robolaunch.models.Organization;
import org.robolaunch.models.Workspace;

public class RequestCreateRobot implements Serializable {
   private Organization organization;
   private String teamId;
   private String region;
   private String cloudInstance;
   private RobotInfo robotInfo;
   private List<Workspace> robotWorkspaces;
   // private List<RobotBuildManagerStep> robotBuildSteps;
   // private List<RobotLaunchManagerLaunchItem> launchManagerLaunchItems;
   private String bufferName;

   public RequestCreateRobot() {
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

   public String getRegion() {
      return region;
   }

   public void setRegion(String region) {
      this.region = region;
   }

   public String getCloudInstance() {
      return cloudInstance;
   }

   public void setCloudInstance(String cloudInstance) {
      this.cloudInstance = cloudInstance;
   }

   public String getBufferName() {
      return bufferName;
   }

   public void setBufferName(String bufferName) {
      this.bufferName = bufferName;
   }

   public RobotInfo getRobotInfo() {
      return robotInfo;
   }

   public void setRobotInfo(RobotInfo robotInfo) {
      this.robotInfo = robotInfo;
   }

   public List<Workspace> getRobotWorkspaces() {
      return robotWorkspaces;
   }

   public void setRobotWorkspaces(List<Workspace> robotWorkspaces) {
      this.robotWorkspaces = robotWorkspaces;
   }

   // public List<RobotBuildManagerStep> getRobotBuildSteps() {
   // return robotBuildSteps;
   // }

   // public void setRobotBuildSteps(List<RobotBuildManagerStep> robotBuildSteps) {
   // this.robotBuildSteps = robotBuildSteps;
   // }

   // public List<RobotLaunchManagerLaunchItem> getLaunchManagerLaunchItems() {
   // return launchManagerLaunchItems;
   // }

   // public void setLaunchManagerLaunchItems(List<RobotLaunchManagerLaunchItem>
   // launchManagerLaunchItems) {
   // this.launchManagerLaunchItems = launchManagerLaunchItems;
   // }

}
