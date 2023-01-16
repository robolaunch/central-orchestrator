package org.robolaunch.models.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.models.Department;

public class ResponseTeams implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<Department> data;

   public ResponseTeams() {
   }

   public ResponseTeams(String message, Boolean success, ArrayList<Department> data) {
      this.message = message;
      this.success = success;
      this.data = data;
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

   public ArrayList<Department> getData() {
      return data;
   }

   public void setData(ArrayList<Department> data) {
      this.data = data;
   }

}
