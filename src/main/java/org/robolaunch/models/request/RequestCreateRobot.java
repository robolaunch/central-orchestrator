package org.robolaunch.models.request;

import java.io.Serializable;

import org.robolaunch.models.Organization;

public class RequestCreateRobot implements Serializable {
   private Organization organization;
   private String teamId;
   private String region;
   private String cloudInstance;
   private Robot robot;
   private String bufferName;

   public RequestCreateRobot() {
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

   public String getRegion() {
      return region;
   }

   public void setRegion(String region) {
      this.region = region;
   }

   public String getCloudInstance() {
      return cloudInstance;
   }

   public void setCloudInstance(String cloudInstance) {
      this.cloudInstance = cloudInstance;
   }

   public Robot getRobot() {
      return robot;
   }

   public void setRobot(Robot robot) {
      this.robot = robot;
   }

   public String getBufferName() {
      return bufferName;
   }

   public void setBufferName(String bufferName) {
      this.bufferName = bufferName;
   }

}
