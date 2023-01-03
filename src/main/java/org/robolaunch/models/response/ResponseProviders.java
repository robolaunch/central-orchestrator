package org.robolaunch.models.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.models.Provider;

public class ResponseProviders implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<Provider> data;

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

   public ArrayList<Provider> getData() {
      return data;
   }

   public void setData(ArrayList<Provider> data) {
      this.data = data;
   }

}
