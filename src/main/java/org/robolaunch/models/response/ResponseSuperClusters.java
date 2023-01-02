package org.robolaunch.models.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.models.SuperCluster;

public class ResponseSuperClusters implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<SuperCluster> data;

   public ResponseSuperClusters() {
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public Boolean getSuccess() {
      return success;
   }

   public void setSuccess(Boolean success) {
      this.success = success;
   }

   public ArrayList<SuperCluster> getData() {
      return data;
   }

   public void setData(ArrayList<SuperCluster> data) {
      this.data = data;
   }

}
