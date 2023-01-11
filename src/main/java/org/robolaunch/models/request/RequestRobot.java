package org.robolaunch.models.request;

import java.io.Serializable;

import org.robolaunch.models.Organization;

public class RequestRobot implements Serializable {
   private RequestCreateRobot robot;
   private String bufferName;
   private String provider;
   private String region;
   private String superCluster;
   private Organization organization;
   private String teamId;
   private String cloudInstanceName;

   public RequestRobot() {
   }

   public RequestCreateRobot getRobot() {
      return robot;
   }

   public void setRobot(RequestCreateRobot robot) {
      this.robot = robot;
   }

   public String getProvider() {
      return provider;
   }

   public void setProvider(String provider) {
      this.provider = provider;
   }

   public String getRegion() {
      return region;
   }

   public void setRegion(String region) {
      this.region = region;
   }

   public String getSuperCluster() {
      return superCluster;
   }

   public void setSuperCluster(String superCluster) {
      this.superCluster = superCluster;
   }

   public String getBufferName() {
      return bufferName;
   }

   public void setBufferName(String bufferName) {
      this.bufferName = bufferName;
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

   public String getCloudInstanceName() {
      return cloudInstanceName;
   }

   public void setCloudInstanceName(String cloudInstanceName) {
      this.cloudInstanceName = cloudInstanceName;
   }
}
