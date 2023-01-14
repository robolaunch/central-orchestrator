package org.robolaunch.models.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.models.Organization;

public class ResponseUserOrganizations implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<Organization> data;

   public ResponseUserOrganizations() {

   }

   public ResponseUserOrganizations(String message, Boolean success, ArrayList<Organization> data) {
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

   public ArrayList<Organization> getData() {
      return data;
   }

   public void setData(ArrayList<Organization> data) {
      this.data = data;
   }

}
