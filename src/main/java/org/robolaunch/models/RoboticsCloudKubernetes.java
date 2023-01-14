package org.robolaunch.models;

import java.io.Serializable;

public class RoboticsCloudKubernetes implements Serializable {
   private String processId;
   private String name;
   private String status;
   private String instanceType;
   private String userStage;
   private String regionName;
   private String organization;
   private String team;
   private String teamName;
   private String bufferName;
   private Integer diskSize;
   private Integer vCPU;
   private Integer GPU;
   private Integer memory;
   private Integer fleets;
   private Integer robots;

   public RoboticsCloudKubernetes() {
   }

   public String getProcessId() {
      return processId;
   }

   public void setProcessId(String processId) {
      this.processId = processId;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getInstanceType() {
      return instanceType;
   }

   public void setInstanceType(String instanceType) {
      this.instanceType = instanceType;
   }

   public String getUserStage() {
      return userStage;
   }

   public void setUserStage(String userStage) {
      this.userStage = userStage;
   }

   public String getRegionName() {
      return regionName;
   }

   public void setRegionName(String regionName) {
      this.regionName = regionName;
   }

   public String getOrganization() {
      return organization;
   }

   public void setOrganization(String organization) {
      this.organization = organization;
   }

   public String getTeam() {
      return team;
   }

   public void setTeam(String team) {
      this.team = team;
   }

   public String getBufferName() {
      return bufferName;
   }

   public void setBufferName(String bufferName) {
      this.bufferName = bufferName;
   }

   public String getTeamName() {
      return teamName;
   }

   public void setTeamName(String teamName) {
      this.teamName = teamName;
   }

   public Integer getDiskSize() {
      return diskSize;
   }

   public void setDiskSize(Integer diskSize) {
      this.diskSize = diskSize;
   }

   public Integer getvCPU() {
      return vCPU;
   }

   public void setvCPU(Integer vCPU) {
      this.vCPU = vCPU;
   }

   public Integer getMemory() {
      return memory;
   }

   public void setMemory(Integer memory) {
      this.memory = memory;
   }

   public Integer getGPU() {
      return GPU;
   }

   public void setGPU(Integer gPU) {
      GPU = gPU;
   }

   public Integer getFleets() {
      return fleets;
   }

   public void setFleets(Integer fleets) {
      this.fleets = fleets;
   }

   public Integer getRobots() {
      return robots;
   }

   public void setRobots(Integer robots) {
      this.robots = robots;
   }

}
