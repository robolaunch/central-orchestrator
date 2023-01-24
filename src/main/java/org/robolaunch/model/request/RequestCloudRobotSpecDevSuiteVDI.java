package org.robolaunch.model.request;

import java.io.Serializable;

public class RequestCloudRobotSpecDevSuiteVDI implements Serializable {
   private String serviceType;
   private Boolean ingress;
   private Boolean privileged;
   private Integer sessionCount;
   private String webrtcPortRange;

   public RequestCloudRobotSpecDevSuiteVDI() {
   }

   public String getServiceType() {
      return serviceType;
   }

   public void setServiceType(String serviceType) {
      this.serviceType = serviceType;
   }

   public Boolean isIngress() {
      return ingress;
   }

   public void setIngress(Boolean ingress) {
      this.ingress = ingress;
   }

   public Integer getSessionCount() {
      return sessionCount;
   }

   public void setSessionCount(Integer sessionCount) {
      this.sessionCount = sessionCount;
   }

   public String getWebrtcPortRange() {
      return webrtcPortRange;
   }

   public void setWebrtcPortRange(String webrtcPortRange) {
      this.webrtcPortRange = webrtcPortRange;
   }

   public Boolean isPrivileged() {
      return privileged;
   }

   public void setPrivileged(Boolean privileged) {
      this.privileged = privileged;
   }

}
