package org.robolaunch.models.request;

import java.util.ArrayList;

public class RequestLaunchManagerSpec {
   private ArrayList<RequestLaunchManagerSpecLaunch> launch;

   public ArrayList<RequestLaunchManagerSpecLaunch> getLaunch() {
      return launch;
   }

   public void setLaunch(ArrayList<RequestLaunchManagerSpecLaunch> launch) {
      this.launch = launch;
   }

}
