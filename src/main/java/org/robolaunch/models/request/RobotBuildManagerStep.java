package org.robolaunch.models.request;

public class RobotBuildManagerStep {
   private String name;
   private String workspace;
   private String command;
   private String script;

   public RobotBuildManagerStep() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getWorkspace() {
      return workspace;
   }

   public void setWorkspace(String workspace) {
      this.workspace = workspace;
   }

   public String getCommand() {
      return command;
   }

   public void setCommand(String command) {
      this.command = command;
   }

   public String getScript() {
      return script;
   }

   public void setScript(String script) {
      this.script = script;
   }

}
