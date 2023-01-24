package org.robolaunch.model.request;

import java.io.Serializable;

public class RequestFleet implements Serializable {
   private RequestCreateFleet fleet;
   private String roboticsCloudProcessId;
   private RequestCreateFederatedFleet federatedFleet;
   private Boolean federated;

   public RequestFleet() {
   }

   public RequestCreateFleet getFleet() {
      return fleet;
   }

   public void setFleet(RequestCreateFleet fleet) {
      this.fleet = fleet;
   }

   public String getRoboticsCloudProcessId() {
      return roboticsCloudProcessId;
   }

   public void setRoboticsCloudProcessId(String roboticsCloudProcessId) {
      this.roboticsCloudProcessId = roboticsCloudProcessId;
   }

   public RequestCreateFederatedFleet getFederatedFleet() {
      return federatedFleet;
   }

   public void setFederatedFleet(RequestCreateFederatedFleet federatedFleet) {
      this.federatedFleet = federatedFleet;
   }

   public Boolean isFederated() {
      return federated;
   }

   public void setFederated(Boolean federated) {
      this.federated = federated;
   }

}
