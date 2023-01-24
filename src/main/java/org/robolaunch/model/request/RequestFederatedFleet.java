package org.robolaunch.model.request;

import java.io.Serializable;

public class RequestFederatedFleet implements Serializable {
   private RequestCreateFederatedFleet fleet;
   private String roboticsCloudProcessId;

   public RequestFederatedFleet() {
   }

   public RequestCreateFederatedFleet getFleet() {
      return fleet;
   }

   public void setFleet(RequestCreateFederatedFleet fleet) {
      this.fleet = fleet;
   }

   public String getRoboticsCloudProcessId() {
      return roboticsCloudProcessId;
   }

   public void setRoboticsCloudProcessId(String roboticsCloudProcessId) {
      this.roboticsCloudProcessId = roboticsCloudProcessId;
   }

}
