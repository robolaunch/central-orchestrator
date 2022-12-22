package org.robolaunch.models.request;

import java.io.Serializable;
import java.util.List;

import org.robolaunch.models.Workspace;

public class Robot implements Serializable {
   private String name;
   private String distro;
   private Integer storage;
   private Boolean isROSBridgeEnabled;

   private Boolean isVDIEnabled;
   private Boolean isIDEEnabled;

   private List<RobotBuildManagerStep> buildManagerSteps;
   private List<RobotLaunchManagerLaunchItem> launchManagerLaunchItems;

   private String workspacesPath;
   private List<Workspace> workspaces;

   public Robot() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDistro() {
      return distro;
   }

   public void setDistro(String distro) {
      this.distro = distro;
   }

   public Integer getStorage() {
      return storage;
   }

   public void setStorage(Integer storage) {
      this.storage = storage;
   }

   public Boolean getIsROSBridgeEnabled() {
      return isROSBridgeEnabled;
   }

   public void setIsROSBridgeEnabled(Boolean isROSBridgeEnabled) {
      this.isROSBridgeEnabled = isROSBridgeEnabled;
   }

   public Boolean getIsVDIEnabled() {
      return isVDIEnabled;
   }

   public void setIsVDIEnabled(Boolean isVDIEnabled) {
      this.isVDIEnabled = isVDIEnabled;
   }

   public Boolean getIsIDEEnabled() {
      return isIDEEnabled;
   }

   public void setIsIDEEnabled(Boolean isIDEEnabled) {
      this.isIDEEnabled = isIDEEnabled;
   }

   public List<RobotBuildManagerStep> getBuildManagerSteps() {
      return buildManagerSteps;
   }

   public void setBuildManagerSteps(List<RobotBuildManagerStep> buildManagerSteps) {
      this.buildManagerSteps = buildManagerSteps;
   }

   public List<RobotLaunchManagerLaunchItem> getLaunchManagerLaunchItems() {
      return launchManagerLaunchItems;
   }

   public void setLaunchManagerLaunchItems(List<RobotLaunchManagerLaunchItem> launchManagerLaunchItems) {
      this.launchManagerLaunchItems = launchManagerLaunchItems;
   }

   public String getWorkspacesPath() {
      return workspacesPath;
   }

   public void setWorkspacesPath(String workspacesPath) {
      this.workspacesPath = workspacesPath;
   }

   public List<Workspace> getWorkspaces() {
      return workspaces;
   }

   public void setWorkspaces(List<Workspace> workspaces) {
      this.workspaces = workspaces;
   }

}
