package org.robolaunch.models;

import java.io.Serializable;
import java.util.Map;

public class LabelList implements Serializable {
   private Map<String, String> myMap;

   public LabelList() {
   }

   public Map<String, String> getMyMap() {
      return myMap;
   }

   public void setMyMap(Map<String, String> mMap) {
      this.myMap = mMap;
   }
}