package org.robolaunch.model.robot;

import java.io.Serializable;

public class CloudRobotRepository implements Serializable {
   private String url;
   private String branch;

   public CloudRobotRepository() {
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public String getBranch() {
      return branch;
   }

   public void setBranch(String branch) {
      this.branch = branch;
   }

}
