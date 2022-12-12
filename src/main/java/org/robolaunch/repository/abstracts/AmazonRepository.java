package org.robolaunch.repository.abstracts;

public interface AmazonRepository {
  public void stopInstance(String nodeName);

  public void startInstance(String nodeName);

  public String getInstanceState(String nodeName);

  public Boolean isInstanceStopped(String nodeName);

  public Boolean isRunning(String nodeName);

}
