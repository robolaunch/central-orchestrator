package org.robolaunch.exception;

public class ApplicationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ApplicationException() {
    super();
  }

  public ApplicationException(String msg) {
    super(msg);
  }

  public ApplicationException(String msg, Exception e) {
    super(msg, e);
  }
}