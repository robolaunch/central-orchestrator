package org.robolaunch.models.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.models.RoboticsCloud;

public class ResponseRoboticsClouds implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<RoboticsCloud> data;

   public ResponseRoboticsClouds() {
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public Boolean getSuccess() {
      return success;
   }

   public void setSuccess(Boolean success) {
      this.success = success;
   }

   public ArrayList<RoboticsCloud> getData() {
      return data;
   }

   public void setData(ArrayList<RoboticsCloud> data) {
      this.data = data;
   }

}
