package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCloudRobotSpecStorage implements Serializable {
   private Integer amount;
   private RequestCloudRobotSpecStorageConfig storageClassConfig;

   public RequestCloudRobotSpecStorage() {
   }

   public Integer getAmount() {
      return amount;
   }

   public void setAmount(Integer amount) {
      this.amount = amount;
   }

   public RequestCloudRobotSpecStorageConfig getStorageClassConfig() {
      return storageClassConfig;
   }

   public void setStorageClassConfig(RequestCloudRobotSpecStorageConfig storageClassConfig) {
      this.storageClassConfig = storageClassConfig;
   }

}
