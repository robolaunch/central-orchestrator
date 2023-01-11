package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCloudRobotSpecDevSuite implements Serializable {
   private Boolean vdiEnabled;
   private Boolean ideEnabled;
   private RequestCloudRobotSpecDevSuiteVDI robotVDITemplate;
   private RequestCloudRobotSpecDevSuiteIDE robotIDETemplate;

   public RequestCloudRobotSpecDevSuite() {
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

   public RequestCloudRobotSpecDevSuiteVDI getRobotVDITemplate() {
      return robotVDITemplate;
   }

   public void setRobotVDITemplate(RequestCloudRobotSpecDevSuiteVDI robotVDITemplate) {
      this.robotVDITemplate = robotVDITemplate;
   }

   public RequestCloudRobotSpecDevSuiteIDE getRobotIDETemplate() {
      return robotIDETemplate;
   }

   public void setRobotIDETemplate(RequestCloudRobotSpecDevSuiteIDE robotIDETemplate) {
      this.robotIDETemplate = robotIDETemplate;
   }

}
