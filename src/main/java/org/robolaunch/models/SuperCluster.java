package org.robolaunch.models;

import java.io.Serializable;

public class SuperCluster implements Serializable {
   private String processId;
   private String name;

   public SuperCluster() {
   }

   public String getProcessId() {
      return processId;
   }

   public void setProcessId(String processId) {
      this.processId = processId;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

}
