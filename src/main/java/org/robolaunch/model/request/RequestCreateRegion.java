
package org.robolaunch.model.request;

import java.io.Serializable;

public class RequestCreateRegion implements Serializable {
   private String name;

   public RequestCreateRegion() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }
}
