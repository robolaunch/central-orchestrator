package org.robolaunch.models.request;

import java.io.Serializable;

public class RobotDevSuiteVDITemplate implements Serializable {
   private String serviceType;
   private Boolean ingress;
   private Boolean privileged;
   private String webRTCPortRange;

   public RobotDevSuiteVDITemplate() {
   }

   public String getServiceType() {
      return serviceType;
   }

   public void setServiceType(String serviceType) {
      this.serviceType = serviceType;
   }

   public Boolean getIngress() {
      return ingress;
   }

   public void setIngress(Boolean ingress) {
      this.ingress = ingress;
   }

   public Boolean getPrivileged() {
      return privileged;
   }

   public void setPrivileged(Boolean privileged) {
      this.privileged = privileged;
   }

   public String getWebRTCPortRange() {
      return webRTCPortRange;
   }

   public void setWebRTCPortRange(String webRTCPortRange) {
      this.webRTCPortRange = webRTCPortRange;
   }

}
