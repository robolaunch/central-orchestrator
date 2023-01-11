package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCloudRobotMetadata implements Serializable {
   private String name;

   public RequestCloudRobotMetadata() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }
}
