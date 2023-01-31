package org.robolaunch.models.request;

import java.io.Serializable;

public class BuildManagerStep implements Serializable {
   private String name;
   private String workspace;
   private String codeType;
   private String code;

   public BuildManagerStep() {
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getWorkspace() {
      return workspace;
   }

   public void setWorkspace(String workspace) {
      this.workspace = workspace;
   }

   public String getCodeType() {
      return codeType;
   }

   public void setCodeType(String codeType) {
      this.codeType = codeType;
   }

   public String getCode() {
      return code;
   }

   public void setCode(String code) {
      this.code = code;
   }

}
