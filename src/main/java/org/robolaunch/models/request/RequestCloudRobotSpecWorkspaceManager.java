package org.robolaunch.models.request;

import java.io.Serializable;
import java.util.List;

import org.robolaunch.models.Workspace;

public class RequestCloudRobotSpecWorkspaceManager implements Serializable {
   private String workspacesPath;
   private List<Workspace> workspaces;

   public RequestCloudRobotSpecWorkspaceManager() {
   }

   public String getWorkspacesPath() {
      return workspacesPath;
   }

   public void setWorkspacesPath(String workspacesPath) {
      this.workspacesPath = workspacesPath;
   }

   public List<Workspace> getWorkspaces() {
      return workspaces;
   }

   public void setWorkspaces(List<Workspace> workspaces) {
      this.workspaces = workspaces;
   }

}
