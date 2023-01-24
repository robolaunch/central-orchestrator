package org.robolaunch.model.request;

import java.io.Serializable;
import java.util.List;

public class RequestCloudRobotSpec implements Serializable {
   private List<String> distributions;
   private RequestCloudRobotSpecStorage storage;
   private RequestCloudRobotSpecDiscoveryServer discoveryServerTemplate;
   private RequestCloudRobotSpecRosBridge rosBridgeTemplate;
   private RequestCloudRobotSpecDevSuite robotDevSuiteTemplate;
   private RequestCloudRobotSpecWorkspaceManager workspaceManagerTemplate;

   public RequestCloudRobotSpec() {
   }

   public RequestCloudRobotSpecStorage getStorage() {
      return storage;
   }

   public void setStorage(RequestCloudRobotSpecStorage storage) {
      this.storage = storage;
   }

   public RequestCloudRobotSpecDiscoveryServer getDiscoveryServerTemplate() {
      return discoveryServerTemplate;
   }

   public void setDiscoveryServerTemplate(RequestCloudRobotSpecDiscoveryServer discoveryServerTemplate) {
      this.discoveryServerTemplate = discoveryServerTemplate;
   }

   public RequestCloudRobotSpecRosBridge getRosBridgeTemplate() {
      return rosBridgeTemplate;
   }

   public void setRosBridgeTemplate(RequestCloudRobotSpecRosBridge rosBridgeTemplate) {
      this.rosBridgeTemplate = rosBridgeTemplate;
   }

   public RequestCloudRobotSpecDevSuite getRobotDevSuiteTemplate() {
      return robotDevSuiteTemplate;
   }

   public void setRobotDevSuiteTemplate(RequestCloudRobotSpecDevSuite robotDevSuiteTemplate) {
      this.robotDevSuiteTemplate = robotDevSuiteTemplate;
   }

   public RequestCloudRobotSpecWorkspaceManager getWorkspaceManagerTemplate() {
      return workspaceManagerTemplate;
   }

   public void setWorkspaceManagerTemplate(RequestCloudRobotSpecWorkspaceManager workspaceManagerTemplate) {
      this.workspaceManagerTemplate = workspaceManagerTemplate;
   }

   public List<String> getDistributions() {
      return distributions;
   }

   public void setDistributions(List<String> distributions) {
      this.distributions = distributions;
   }

}
