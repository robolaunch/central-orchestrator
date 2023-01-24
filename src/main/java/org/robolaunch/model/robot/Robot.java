package org.robolaunch.model.robot;

import java.io.Serializable;
import java.util.List;

public class Robot implements Serializable {
   private String processId;
   private String name;
   private String type;
   private String status;
   private List<String> distributions;
   private Integer storage;
   private Boolean bridgeEnabled;
   private String image;
   private Boolean vdiEnabled;
   private Boolean ideEnabled;
   private Integer vdiSessionCount;
   private String workspacesPath;
   private List<Workspace> workspaces;
   private String rosBridgeUrl;
   private String vdiUrl;
   private String ideUrl;
   private String teamName;
   private String roboticsCloudName;
   private String fleetName;

   public Robot() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public List<String> getDistributions() {
      return distributions;
   }

   public void setDistributions(List<String> distributions) {
      this.distributions = distributions;
   }

   public Integer getStorage() {
      return storage;
   }

   public void setStorage(Integer storage) {
      this.storage = storage;
   }

   public Boolean isBridgeEnabled() {
      return bridgeEnabled;
   }

   public void setBridgeEnabled(Boolean bridgeEnabled) {
      this.bridgeEnabled = bridgeEnabled;
   }

   public String getImage() {
      return image;
   }

   public void setImage(String image) {
      this.image = image;
   }

   public Boolean isVdiEnabled() {
      return vdiEnabled;
   }

   public void setVdiEnabled(Boolean vdiEnabled) {
      this.vdiEnabled = vdiEnabled;
   }

   public Boolean isIdeEnabled() {
      return ideEnabled;
   }

   public void setIdeEnabled(Boolean ideEnabled) {
      this.ideEnabled = ideEnabled;
   }

   public Integer getVdiSessionCount() {
      return vdiSessionCount;
   }

   public void setVdiSessionCount(Integer vdiSessionCount) {
      this.vdiSessionCount = vdiSessionCount;
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

   public String getProcessId() {
      return processId;
   }

   public void setProcessId(String processId) {
      this.processId = processId;
   }

   public String getRosBridgeUrl() {
      return rosBridgeUrl;
   }

   public void setRosBridgeUrl(String rosBridgeUrl) {
      this.rosBridgeUrl = rosBridgeUrl;
   }

   public String getVdiUrl() {
      return vdiUrl;
   }

   public void setVdiUrl(String vdiUrl) {
      this.vdiUrl = vdiUrl;
   }

   public String getIdeUrl() {
      return ideUrl;
   }

   public void setIdeUrl(String ideUrl) {
      this.ideUrl = ideUrl;
   }

   public String getTeamName() {
      return teamName;
   }

   public void setTeamName(String teamName) {
      this.teamName = teamName;
   }

   public String getRoboticsCloudName() {
      return roboticsCloudName;
   }

   public void setRoboticsCloudName(String roboticsCloudName) {
      this.roboticsCloudName = roboticsCloudName;
   }

   public String getFleetName() {
      return fleetName;
   }

   public void setFleetName(String fleetName) {
      this.fleetName = fleetName;
   }

}
