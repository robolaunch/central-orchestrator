package org.robolaunch.models.response;

public class PlainResponse {
   private String message;
   private Boolean success;

   public PlainResponse() {
   }

   public PlainResponse(String message, Boolean success) {
      this.message = message;
      this.success = success;
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

}
