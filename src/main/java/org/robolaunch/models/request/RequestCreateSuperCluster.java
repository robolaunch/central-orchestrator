package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCreateSuperCluster implements Serializable {
   private String name;

   public RequestCreateSuperCluster() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

}
