package org.robolaunch.models.request;

public class RobotDevSuiteIDETemplate {
   private String serviceType;
   private Boolean ingress;
   private Boolean privileged;

   public RobotDevSuiteIDETemplate() {
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

}
