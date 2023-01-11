package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCloudRobotSpecStorageConfig implements Serializable {
   private String name;
   private String accessMode;

   public RequestCloudRobotSpecStorageConfig() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getAccessMode() {
      return accessMode;
   }

   public void setAccessMode(String accessMode) {
      this.accessMode = accessMode;
   }

}
