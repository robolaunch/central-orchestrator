package org.robolaunch.model.request;

import java.io.Serializable;
import java.util.ArrayList;

public class RequestCreateFederatedFleet implements Serializable {
   private String name;
   private ArrayList<String> clusters;

   public RequestCreateFederatedFleet() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public ArrayList<String> getClusters() {
      return clusters;
   }

   public void setClusters(ArrayList<String> clusters) {
      this.clusters = clusters;
   }

}
