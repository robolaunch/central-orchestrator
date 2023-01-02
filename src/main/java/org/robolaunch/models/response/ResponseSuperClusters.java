package org.robolaunch.models.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.models.SuperClusterKubernetes;

public class ResponseSuperClusters implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<SuperClusterKubernetes> data;

   public ResponseSuperClusters() {
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

   public ArrayList<SuperClusterKubernetes> getData() {
      return data;
   }

   public void setData(ArrayList<SuperClusterKubernetes> data) {
      this.data = data;
   }

}
