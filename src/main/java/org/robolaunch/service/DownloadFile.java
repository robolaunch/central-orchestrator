package org.robolaunch.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/download")
public class DownloadFile extends HttpServlet {

   protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      // create a temporary file
      File tempFile = File.createTempFile("robotScript", ".tmp");
      try (PrintWriter out = new PrintWriter(new FileOutputStream(tempFile))) {
         out.println("This is the content of the temporary file.");
         out.println("another thing()");
      }

      // set the content type and attachment header
      response.setContentType("application/octet-stream");
      response.setHeader("Content-Disposition", "attachment; filename=\"" + tempFile.getName() + "\"");

      // transfer the file
      try (FileOutputStream fileOut = new FileOutputStream(tempFile)) {
         fileOut.write(tempFile.getName().getBytes());
      }
      System.out.println("downloaded the file!");
      // delete the temporary file
      tempFile.delete();
   }
}