package org.robolaunch.models.request;

import java.io.Serializable;

import org.robolaunch.models.Organization;

public class RequestFleet implements Serializable {
   private RequestCreateFleet fleet;
   private Organization organization;
   private String teamId;
   private String region;
   private String bufferName;
   private String provider;
   private String superCluster;
   private String cloudInstanceName;

   public RequestFleet() {
   }

   public RequestCreateFleet getFleet() {
      return fleet;
   }

   public void setFleet(RequestCreateFleet fleet) {
      this.fleet = fleet;
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

   public String getBufferName() {
      return bufferName;
   }

   public void setBufferName(String bufferName) {
      this.bufferName = bufferName;
   }

   public String getCloudInstanceName() {
      return cloudInstanceName;
   }

   public void setCloudInstanceName(String cloudInstanceName) {
      this.cloudInstanceName = cloudInstanceName;
   }

   public String getSuperCluster() {
      return superCluster;
   }

   public void setSuperCluster(String superCluster) {
      this.superCluster = superCluster;
   }

   public String getProvider() {
      return provider;
   }

   public void setProvider(String provider) {
      this.provider = provider;
   }

}
