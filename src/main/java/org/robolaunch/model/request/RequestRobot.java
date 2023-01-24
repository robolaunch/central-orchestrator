package org.robolaunch.model.request;

import java.io.Serializable;

public class RequestRobot implements Serializable {
   private RequestCreateRobot robot;
   private String fleetProcessId;
   private Boolean federated;
   private RequestCreateFederatedRobot federatedRobot;

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

   public Boolean isFederated() {
      return federated;
   }

   public void setFederated(Boolean federated) {
      this.federated = federated;
   }

   public RequestCreateFederatedRobot getFederatedRobot() {
      return federatedRobot;
   }

   public void setFederatedRobot(RequestCreateFederatedRobot federatedRobot) {
      this.federatedRobot = federatedRobot;
   }

}
