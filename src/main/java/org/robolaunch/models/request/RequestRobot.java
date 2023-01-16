package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestRobot implements Serializable {
   private RequestCreateRobot robot;
   private String fleetProcessId;

   public RequestRobot() {
   }

   public RequestCreateRobot getRobot() {
      return robot;
   }

   public void setRobot(RequestCreateRobot robot) {
      this.robot = robot;
   }

   public String getFleetProcessId() {
      return fleetProcessId;
   }

   public void setFleetProcessId(String fleetProcessId) {
      this.fleetProcessId = fleetProcessId;
   }

}
