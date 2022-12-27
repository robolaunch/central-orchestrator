package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCreateRoboticsCloud implements Serializable {
   private String name;

   public RequestCreateRoboticsCloud() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

}
