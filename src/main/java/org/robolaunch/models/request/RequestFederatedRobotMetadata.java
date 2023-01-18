package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestFederatedRobotMetadata implements Serializable {
   private String name;
   private String namespace;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getNamespace() {
      return namespace;
   }

   public void setNamespace(String namespace) {
      this.namespace = namespace;
   }

}
