package org.robolaunch.models.request;

public class RequestLaunchManagerSpecLaunch {
   private String name;
   private String physicalInstance;
   private String cloudInstance;
   private String workspace;
   private String repository;
   private Boolean isNamespacing;
   private String launchFilePath;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getPhysicalInstance() {
      return physicalInstance;
   }

   public void setPhysicalInstance(String physicalInstance) {
      this.physicalInstance = physicalInstance;
   }

   public String getCloudInstance() {
      return cloudInstance;
   }

   public void setCloudInstance(String cloudInstance) {
      this.cloudInstance = cloudInstance;
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

   public Boolean getIsNamespacing() {
      return isNamespacing;
   }

   public void setIsNamespacing(Boolean isNamespacing) {
      this.isNamespacing = isNamespacing;
   }

   public String getLaunchFilePath() {
      return launchFilePath;
   }

   public void setLaunchFilePath(String launchFilePath) {
      this.launchFilePath = launchFilePath;
   }

}
