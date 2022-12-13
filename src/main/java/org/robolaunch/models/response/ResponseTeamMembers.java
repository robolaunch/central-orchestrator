package org.robolaunch.models.response;

import java.util.ArrayList;

import org.robolaunch.models.GroupMember;

public class ResponseTeamMembers {
   private String message;
   private Boolean success;
   private ArrayList<GroupMember> data;

   public ResponseTeamMembers() {
   }

   public ResponseTeamMembers(String message, Boolean success, ArrayList<GroupMember> data) {
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

   public ArrayList<GroupMember> getData() {
      return data;
   }

   public void setData(ArrayList<GroupMember> data) {
      this.data = data;
   }

}
