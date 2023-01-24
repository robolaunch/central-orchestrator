package org.robolaunch.model.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.model.account.Team;

public class ResponseTeams implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<Team> data;

   public ResponseTeams() {
   }

   public ResponseTeams(String message, Boolean success, ArrayList<Team> data) {
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

   public ArrayList<Team> getData() {
      return data;
   }

   public void setData(ArrayList<Team> data) {
      this.data = data;
   }

}
