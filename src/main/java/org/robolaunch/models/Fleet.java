package org.robolaunch.models;

import java.io.Serializable;

public class Fleet implements Serializable {
   private String name;
   private String processId;
   private String fleetStatus;
   private String teamName;
   private String roboticsCloudName;
   private String type;

   public Fleet() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getProcessId() {
      return processId;
   }

   public void setProcessId(String processId) {
      this.processId = processId;
   }

   public String getFleetStatus() {
      return fleetStatus;
   }

   public void setFleetStatus(String fleetStatus) {
      this.fleetStatus = fleetStatus;
   }

   public String getTeamName() {
      return teamName;
   }

   public void setTeamName(String teamName) {
      this.teamName = teamName;
   }

   public String getRoboticsCloudName() {
      return roboticsCloudName;
   }

   public void setRoboticsCloudName(String roboticsCloudName) {
      this.roboticsCloudName = roboticsCloudName;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

}
