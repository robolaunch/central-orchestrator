package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCreateFleet implements Serializable {
   private String name;

   public RequestCreateFleet() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }
}
