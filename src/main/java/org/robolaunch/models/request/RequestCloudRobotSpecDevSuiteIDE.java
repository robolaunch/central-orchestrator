package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCloudRobotSpecDevSuiteIDE implements Serializable {
   private String serviceType;
   private Boolean ingress;
   private Boolean privileged;

   public RequestCloudRobotSpecDevSuiteIDE() {
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

   public Boolean isPrivileged() {
      return privileged;
   }

   public void setPrivileged(Boolean privileged) {
      this.privileged = privileged;
   }

}
