package org.robolaunch.models.request;

import java.io.Serializable;

public class RequestCloudRobotSpecRosBridge implements Serializable {
   private RequestCloudRobotSpecRosBridgeRos ros;
   private RequestCloudRobotSpecRosBridgeRos ros2;
   private String image;

   public RequestCloudRobotSpecRosBridge() {
   }

   public RequestCloudRobotSpecRosBridgeRos getRos() {
      return ros;
   }

   public void setRos(RequestCloudRobotSpecRosBridgeRos ros) {
      this.ros = ros;
   }

   public RequestCloudRobotSpecRosBridgeRos getRos2() {
      return ros2;
   }

   public void setRos2(RequestCloudRobotSpecRosBridgeRos ros2) {
      this.ros2 = ros2;
   }

   public String getImage() {
      return image;
   }

   public void setImage(String image) {
      this.image = image;
   }

}
