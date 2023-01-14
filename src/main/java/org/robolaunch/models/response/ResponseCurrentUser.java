package org.robolaunch.models.response;

import java.io.Serializable;

import org.robolaunch.models.CurrentUser;

public class ResponseCurrentUser implements Serializable {
   private String message;
   private Boolean success;
   private CurrentUser user;

   public ResponseCurrentUser() {
   }

   public ResponseCurrentUser(String message, Boolean success, CurrentUser user) {
      this.message = message;
      this.success = success;
      this.user = user;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public Boolean isSuccess() {
      return success;
   }

   public void setSuccess(Boolean success) {
      this.success = success;
   }

   public CurrentUser getUser() {
      return user;
   }

   public void setUser(CurrentUser user) {
      this.user = user;
   }
}
