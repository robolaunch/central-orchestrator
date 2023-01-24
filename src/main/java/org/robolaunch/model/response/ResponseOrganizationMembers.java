package org.robolaunch.model.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.model.account.UserGroupMember;

public class ResponseOrganizationMembers implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<UserGroupMember> data;

   public ResponseOrganizationMembers() {
   }

   public ResponseOrganizationMembers(String message, Boolean success, ArrayList<UserGroupMember> data) {
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

   public ArrayList<UserGroupMember> getData() {
      return data;
   }

   public void setData(ArrayList<UserGroupMember> data) {
      this.data = data;
   }

}
