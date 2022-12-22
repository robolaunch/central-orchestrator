package org.robolaunch.models.request;

import java.io.Serializable;
import java.util.List;

public class RobotBuildManager implements Serializable {
   private String name;
   private String targetRobot;
   private List<RobotBuildManagerStep> steps;

   public RobotBuildManager() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getTargetRobot() {
      return targetRobot;
   }

   public void setTargetRobot(String targetRobot) {
      this.targetRobot = targetRobot;
   }

   public List<RobotBuildManagerStep> getSteps() {
      return steps;
   }

   public void setSteps(List<RobotBuildManagerStep> steps) {
      this.steps = steps;
   }

}
