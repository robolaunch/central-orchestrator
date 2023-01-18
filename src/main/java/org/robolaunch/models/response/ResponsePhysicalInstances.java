package org.robolaunch.models.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.models.PhysicalInstanceKubernetes;

public class ResponsePhysicalInstances implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<PhysicalInstanceKubernetes> data;

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

   public ArrayList<PhysicalInstanceKubernetes> getData() {
      return data;
   }

   public void setData(ArrayList<PhysicalInstanceKubernetes> data) {
      this.data = data;
   }

}
