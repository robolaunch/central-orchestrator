package org.robolaunch.models.request;

import java.io.Serializable;

public class RobotDevSuite implements Serializable {
   private String name;
   private String targetRobot;
   private Boolean vdiEnabled;
   private Boolean ideEnabled;
   private RobotDevSuiteVDITemplate vdiTemplate;
   private RobotDevSuiteIDETemplate ideTemplate;

   public RobotDevSuite() {
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

   public Boolean isVdiEnabled() {
      return vdiEnabled;
   }

   public void setVdiEnabled(Boolean vdiEnabled) {
      this.vdiEnabled = vdiEnabled;
   }

   public Boolean isIdeEnabled() {
      return ideEnabled;
   }

   public void setIdeEnabled(Boolean ideEnabled) {
      this.ideEnabled = ideEnabled;
   }

   public RobotDevSuiteVDITemplate getVdiTemplate() {
      return vdiTemplate;
   }

   public void setVdiTemplate(RobotDevSuiteVDITemplate vdiTemplate) {
      this.vdiTemplate = vdiTemplate;
   }

   public RobotDevSuiteIDETemplate getIdeTemplate() {
      return ideTemplate;
   }

   public void setIdeTemplate(RobotDevSuiteIDETemplate ideTemplate) {
      this.ideTemplate = ideTemplate;
   }

}
