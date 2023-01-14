package org.robolaunch.models.request;

import java.util.ArrayList;

public class RequestBuildManagerSpec {
   private ArrayList<BuildManagerStep> steps;

   public ArrayList<BuildManagerStep> getSteps() {
      return steps;
   }

   public void setSteps(ArrayList<BuildManagerStep> steps) {
      this.steps = steps;
   }

}
