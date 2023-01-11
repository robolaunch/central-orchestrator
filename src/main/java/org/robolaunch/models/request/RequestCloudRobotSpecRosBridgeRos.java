package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCloudRobotSpecRosBridgeRos implements Serializable {
   private Boolean enabled;
   private String distro;

   public RequestCloudRobotSpecRosBridgeRos() {
   }

   public Boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
   }

   public String getDistro() {
      return distro;
   }

   public void setDistro(String distro) {
      this.distro = distro;
   }

}
