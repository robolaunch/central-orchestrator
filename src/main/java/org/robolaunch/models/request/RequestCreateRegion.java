
package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCreateRegion implements Serializable {
   private String provider;
   private String name;

   public RequestCreateRegion() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getProvider() {
      return provider;
   }

   public void setProvider(String provider) {
      this.provider = provider;
   }

}
