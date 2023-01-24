package org.robolaunch.model.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.model.robot.ProviderKubernetes;

public class ResponseProviders implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<ProviderKubernetes> data;

   public ResponseProviders() {
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

   public ArrayList<ProviderKubernetes> getData() {
      return data;
   }

   public void setData(ArrayList<ProviderKubernetes> data) {
      this.data = data;
   }

}
