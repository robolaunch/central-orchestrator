package org.robolaunch.repository.abstracts;

public interface AmazonRepository {
  public void stopInstance(String nodeName, String provider, String region, String superCluster);

  public void startInstance(String nodeName, String provider, String region, String superCluster);

  public String getInstanceState(String nodeName, String provider, String region, String superCluster);

  public Boolean isInstanceStopped(String nodeName, String provider, String region, String superCluster);

  public Boolean isRunning(String nodeName, String provider, String region, String superCluster);

}
