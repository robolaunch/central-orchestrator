package org.robolaunch;

import io.quarkus.runtime.annotations.QuarkusMain;

import org.robolaunch.exception.GlobalExceptionHandler;

import io.quarkus.runtime.Quarkus;

@QuarkusMain
public class Main {
  public static void main(String... args) {
    Quarkus.run(args);
    Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
  }
}
