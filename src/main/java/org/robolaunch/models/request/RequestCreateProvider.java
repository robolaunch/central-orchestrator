package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCreateProvider implements Serializable {
   private String name;

   public RequestCreateProvider() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

}
