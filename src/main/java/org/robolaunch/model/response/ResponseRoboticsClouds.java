package org.robolaunch.model.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.model.robot.RoboticsCloudKubernetes;

public class ResponseRoboticsClouds implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<RoboticsCloudKubernetes> data;

   public ResponseRoboticsClouds() {
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public Boolean isSuccess() {
      return success;
   }

   public void setSuccess(Boolean success) {
      this.success = success;
   }

   public ArrayList<RoboticsCloudKubernetes> getData() {
      return data;
   }

   public void setData(ArrayList<RoboticsCloudKubernetes> data) {
      this.data = data;
   }

}
