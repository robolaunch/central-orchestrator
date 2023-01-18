package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestFederatedRobotTemplateSpecDiscoveryServer implements Serializable {
   private String type;
   private String cluster;
   private RequestFederatedRobotReference reference;
   private String hostname;
   private String subdomain;

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public String getCluster() {
      return cluster;
   }

   public void setCluster(String cluster) {
      this.cluster = cluster;
   }

   public RequestFederatedRobotReference getReference() {
      return reference;
   }

   public void setReference(RequestFederatedRobotReference reference) {
      this.reference = reference;
   }

   public String getHostname() {
      return hostname;
   }

   public void setHostname(String hostname) {
      this.hostname = hostname;
   }

   public String getSubdomain() {
      return subdomain;
   }

   public void setSubdomain(String subdomain) {
      this.subdomain = subdomain;
   }

}
