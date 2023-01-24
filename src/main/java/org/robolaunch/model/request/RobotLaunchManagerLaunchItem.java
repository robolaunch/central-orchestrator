package org.robolaunch.model.request;

import java.io.Serializable;

public class RobotLaunchManagerLaunchItem implements Serializable {
   private String name;
   private String cluster;
   private String clusterName;
   private String workspace;
   private String repository;
   private Boolean namespacing;
   private String launchFilePath;
   private String workspacesPath;

   public RobotLaunchManagerLaunchItem() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getCluster() {
      return cluster;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public String getWorkspace() {
      return workspace;
   }

   public void setWorkspace(String workspace) {
      this.workspace = workspace;
   }

   public String getRepository() {
      return repository;
   }

   public void setRepository(String repository) {
      this.repository = repository;
   }

   public boolean isNamespacing() {
      return namespacing;
   }

   public void setNamespacing(boolean namespacing) {
      this.namespacing = namespacing;
   }

   public String getLaunchFilePath() {
      return launchFilePath;
   }

   public void setLaunchFilePath(String launchFilePath) {
      this.launchFilePath = launchFilePath;
   }

   public String getClusterName() {
      return clusterName;
   }

   public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
   }

   public String getWorkspacesPath() {
      return workspacesPath;
   }

   public void setWorkspacesPath(String workspacesPath) {
      this.workspacesPath = workspacesPath;
   }

}
