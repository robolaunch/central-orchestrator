package org.robolaunch.model.response;

import org.robolaunch.model.account.LoginResponse;

public class ResponseRefreshToken {
   private String message;
   private Boolean success;
   private LoginResponse data;

   public ResponseRefreshToken() {
   }

   public ResponseRefreshToken(String message, Boolean success, LoginResponse data) {
      this.message = message;
      this.success = success;
      this.data = data;
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

   public LoginResponse getData() {
      return data;
   }

   public void setData(LoginResponse data) {
      this.data = data;
   }
}
