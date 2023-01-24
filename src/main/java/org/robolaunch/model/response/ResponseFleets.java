package org.robolaunch.model.response;

import java.util.ArrayList;

import org.robolaunch.model.robot.Fleet;

public class ResponseFleets {
   private String message;
   private Boolean success;
   private ArrayList<Fleet> data;

   public ResponseFleets() {
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

   public ArrayList<Fleet> getData() {
      return data;
   }

   public void setData(ArrayList<Fleet> data) {
      this.data = data;
   }

}
