package org.robolaunch.models.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.models.CloudInstance;

public class ResponseCloudInstances implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<CloudInstance> data;

   public ResponseCloudInstances() {
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

   public ArrayList<CloudInstance> getData() {
      return data;
   }

   public void setData(ArrayList<CloudInstance> data) {
      this.data = data;
   }

}
