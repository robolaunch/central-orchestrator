package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestLaunchManagerSpecLaunch implements Serializable {
   private String name;
   private String workspace;
   private String repository;
   private Boolean namespacing;
   private String launchFilePath;

   public RequestLaunchManagerSpecLaunch() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
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

   public String getLaunchFilePath() {
      return launchFilePath;
   }

   public void setLaunchFilePath(String launchFilePath) {
      this.launchFilePath = launchFilePath;
   }

   public Boolean isNamespacing() {
      return namespacing;
   }

   public void setNamespacing(Boolean namespacing) {
      this.namespacing = namespacing;
   }
}
