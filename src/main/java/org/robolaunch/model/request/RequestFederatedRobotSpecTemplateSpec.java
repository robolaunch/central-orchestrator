package org.robolaunch.model.request;

import java.io.Serializable;
import java.util.ArrayList;

public class RequestFederatedRobotSpecTemplateSpec implements Serializable {
   private ArrayList<String> distributions;
   private RequestCloudRobotSpecStorage storage;
   private RequestFederatedRobotTemplateSpecDiscoveryServer discoveryServerTemplate;
   private RequestCloudRobotSpecRosBridge rosBridgeTemplate;
   private RequestCloudRobotSpecDevSuite robotDevSuiteTemplate;
   private RequestCloudRobotSpecWorkspaceManager workspaceManagerTemplate;
   private ArrayList<String> clusters;

   public ArrayList<String> getDistributions() {
      return distributions;
   }

   public void setDistributions(ArrayList<String> distributions) {
      this.distributions = distributions;
   }

   public RequestCloudRobotSpecStorage getStorage() {
      return storage;
   }

   public void setStorage(RequestCloudRobotSpecStorage storage) {
      this.storage = storage;
   }

   public RequestFederatedRobotTemplateSpecDiscoveryServer getDiscoveryServerTemplate() {
      return discoveryServerTemplate;
   }

   public void setDiscoveryServerTemplate(RequestFederatedRobotTemplateSpecDiscoveryServer discoveryServerTemplate) {
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

   public ArrayList<String> getClusters() {
      return clusters;
   }

   public void setClusters(ArrayList<String> clusters) {
      this.clusters = clusters;
   }

}
