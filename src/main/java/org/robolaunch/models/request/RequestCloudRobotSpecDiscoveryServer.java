package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCloudRobotSpecDiscoveryServer implements Serializable {
   private String type;
   private String cluster;
   private String hostname;
   private String subdomain;

   public RequestCloudRobotSpecDiscoveryServer() {
   }

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
