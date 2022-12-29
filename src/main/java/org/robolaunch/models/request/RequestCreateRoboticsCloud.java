package org.robolaunch.models.request;

import java.io.Serializable;

import org.robolaunch.models.Organization;

public class RequestCreateRoboticsCloud implements Serializable {
   private Organization organization;
   private String teamId;
   private String cloudInstanceName;
   private String instanceType;

   public RequestCreateRoboticsCloud() {
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

   public String getInstanceType() {
      return instanceType;
   }

   public void setInstanceType(String instanceType) {
      this.instanceType = instanceType;
   }

}
