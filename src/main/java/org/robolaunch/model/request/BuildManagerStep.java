package org.robolaunch.model.request;

import java.io.Serializable;

public class BuildManagerStep implements Serializable {
   private String name;
   private String workspace;
   private String command;
   private String script;

   public BuildManagerStep() {
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
