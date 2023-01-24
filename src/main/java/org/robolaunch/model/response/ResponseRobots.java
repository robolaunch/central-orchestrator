package org.robolaunch.model.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.model.robot.Robot;

public class ResponseRobots implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<Robot> data;

   public ResponseRobots() {
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

   public ArrayList<Robot> getData() {
      return data;
   }

   public void setData(ArrayList<Robot> data) {
      this.data = data;
   }

}
