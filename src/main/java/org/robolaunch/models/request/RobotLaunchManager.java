package org.robolaunch.models.request;

import java.io.Serializable;
import java.util.List;

public class RobotLaunchManager implements Serializable {
   private String targetRobot;
   private String targetVDI;
   private List<RobotLaunchManagerLaunchItem> launchItems;

   public RobotLaunchManager() {
   }

   public String getTargetRobot() {
      return targetRobot;
   }

   public void setTargetRobot(String targetRobot) {
      this.targetRobot = targetRobot;
   }

   public String getTargetVDI() {
      return targetVDI;
   }

   public void setTargetVDI(String targetVDI) {
      this.targetVDI = targetVDI;
   }

   public List<RobotLaunchManagerLaunchItem> getLaunchItems() {
      return launchItems;
   }

   public void setLaunchItems(List<RobotLaunchManagerLaunchItem> launchItems) {
      this.launchItems = launchItems;
   }

}
