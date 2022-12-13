package org.robolaunch.models;

import java.io.Serializable;

public class ResponseEntity implements Serializable {
   private String message;
   private String status;
   private Object data;

   public ResponseEntity() {
   }

   public ResponseEntity(String message, String status, Object data) {
      this.message = message;
      this.status = status;
      this.data = data;
   }

   public String getMessage() {
      return message;
   }
   public void setMessage(String message) {
      this.message = message;
   }
   public String getStatus() {
      return status;
   }
   public void setStatus(String status) {
      this.status = status;
   }
   public Object getData() {
      return data;
   }
   public void setData(Object data) {
      this.data = data;
   }

   
}
