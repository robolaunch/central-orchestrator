package org.robolaunch.model.robot;

import java.io.Serializable;

public class SuperClusterKubernetes implements Serializable {
   private String processId;
   private String name;

   public SuperClusterKubernetes() {
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
