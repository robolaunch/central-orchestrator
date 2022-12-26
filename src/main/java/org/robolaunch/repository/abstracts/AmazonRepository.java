package org.robolaunch.repository.abstracts;

public interface AmazonRepository {
  public void stopInstance(String nodeName, String region);

  public void startInstance(String nodeName, String region);

  public String getInstanceState(String nodeName, String region);

  public Boolean isInstanceStopped(String nodeName, String region);

  public Boolean isRunning(String nodeName, String region);

}
