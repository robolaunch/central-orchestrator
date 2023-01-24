package org.robolaunch.model.response;

import java.io.Serializable;
import java.util.ArrayList;

import org.robolaunch.model.robot.RegionKubernetes;

public class ResponseRegions implements Serializable {
   private String message;
   private Boolean success;
   private ArrayList<RegionKubernetes> data;

   public ResponseRegions() {
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

   public ArrayList<RegionKubernetes> getData() {
      return data;
   }

   public void setData(ArrayList<RegionKubernetes> data) {
      this.data = data;
   }

}
