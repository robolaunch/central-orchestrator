package org.robolaunch.exception;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
   public void uncaughtException(Thread t, Throwable e) {
      System.out.println("Uncaught exception: " + e);
   }
}