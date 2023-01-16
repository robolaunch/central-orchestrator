package org.robolaunch.models.response;

import java.util.ArrayList;

import org.robolaunch.models.Fleet;

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
