package org.robolaunch.model.request;

import java.io.Serializable;

public class RobotInfo implements Serializable {
   private String robotType;
   private String fleetName;
   private String robotName;
   private String cloudInstance;
   private String physicalInstance;
   private Integer robotCount;
   private Integer robotStorage;
   private Boolean robotRos1Bridge;
   private Boolean robotRos2Bridge;
   private Boolean robotIDE;
   private Boolean robotVDI;

   public RobotInfo() {
   }

   public String getRobotType() {
      return robotType;
   }

   public void setRobotType(String robotType) {
      this.robotType = robotType;
   }

   public String getFleetName() {
      return fleetName;
   }

   public void setFleetName(String fleetName) {
      this.fleetName = fleetName;
   }

   public String getRobotName() {
      return robotName;
   }

   public void setRobotName(String robotName) {
      this.robotName = robotName;
   }

   public String getCloudInstance() {
      return cloudInstance;
   }

   public void setCloudInstance(String cloudInstance) {
      this.cloudInstance = cloudInstance;
   }

   public String getPhysicalInstance() {
      return physicalInstance;
   }

   public void setPhysicalInstance(String physicalInstance) {
      this.physicalInstance = physicalInstance;
   }

   public Integer getRobotCount() {
      return robotCount;
   }

   public void setRobotCount(Integer robotCount) {
      this.robotCount = robotCount;
   }

   public Integer getRobotStorage() {
      return robotStorage;
   }

   public void setRobotStorage(Integer robotStorage) {
      this.robotStorage = robotStorage;
   }

   public Boolean isRobotRos1Bridge() {
      return robotRos1Bridge;
   }

   public void setRobotRos1Bridge(Boolean robotRos1Bridge) {
      this.robotRos1Bridge = robotRos1Bridge;
   }

   public Boolean isRobotRos2Bridge() {
      return robotRos2Bridge;
   }

   public void setRobotRos2Bridge(Boolean robotRos2Bridge) {
      this.robotRos2Bridge = robotRos2Bridge;
   }

   public Boolean isRobotIDE() {
      return robotIDE;
   }

   public void setRobotIDE(Boolean robotIDE) {
      this.robotIDE = robotIDE;
   }

   public Boolean isRobotVDI() {
      return robotVDI;
   }

   public void setRobotVDI(Boolean robotVDI) {
      this.robotVDI = robotVDI;
   }

}
