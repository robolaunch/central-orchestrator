package org.robolaunch.models.request;

import java.io.Serializable;
import java.util.List;

import org.robolaunch.models.Workspace;

public class Robot implements Serializable {
   private RobotInfo robotInfo;
   private List<Workspace> robotWorkspaces;
   private List<RobotBuildManagerStep> robotBuildSteps;
   private List<RobotLaunchManagerLaunchItem> launchManagerLaunchItems;

   public Robot() {
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

   public List<RobotBuildManagerStep> getRobotBuildSteps() {
      return robotBuildSteps;
   }

   public void setRobotBuildSteps(List<RobotBuildManagerStep> robotBuildSteps) {
      this.robotBuildSteps = robotBuildSteps;
   }

   public List<RobotLaunchManagerLaunchItem> getLaunchManagerLaunchItems() {
      return launchManagerLaunchItems;
   }

   public void setLaunchManagerLaunchItems(List<RobotLaunchManagerLaunchItem> launchManagerLaunchItems) {
      this.launchManagerLaunchItems = launchManagerLaunchItems;
   }

}
