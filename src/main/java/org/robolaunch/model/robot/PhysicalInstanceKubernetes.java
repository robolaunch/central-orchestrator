package org.robolaunch.model.robot;

import java.io.Serializable;
import java.util.ArrayList;

public class PhysicalInstanceKubernetes implements Serializable {
   private String name;
   private String status;
   private ArrayList<String> subnets;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public ArrayList<String> getSubnets() {
      return subnets;
   }

   public void setSubnets(ArrayList<String> subnets) {
      this.subnets = subnets;
   }

}
