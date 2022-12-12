package org.robolaunch.repository.concretes;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.robolaunch.repository.abstracts.AmazonRepository;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;

@ApplicationScoped
public class AmazonRepositoryImpl implements AmazonRepository {
  private Ec2Client ec2Client;

  @ConfigProperty(name = "quarkus.aws.access.key.id")
  String accessKeyId;

  @ConfigProperty(name = "quarkus.aws.secret.access.key")
  String secretAccessKey;

  @PostConstruct
  public void ec2Client() {
    Region region = Region.EU_CENTRAL_1;
    AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
        accessKeyId,
        secretAccessKey);
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCreds);
    this.ec2Client = Ec2Client.builder().region(region).credentialsProvider(credentialsProvider).build();

  }

  @Override
  public void stopInstance(String nodeName) {
    DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().build();
    DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest);

    for (var reservation : describeInstancesResponse.reservations()) {
      for (var instance : reservation.instances()) {
        if (instance.privateDnsName().equals(nodeName)) {
          StopInstancesRequest stopInstancesRequest = StopInstancesRequest.builder().instanceIds(instance.instanceId())
              .build();
          ec2Client.stopInstances(stopInstancesRequest);
          break;
        }

      }
    }
  }

  @Override
  public void startInstance(String nodeName) {
    DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().build();
    DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest);

    for (var reservation : describeInstancesResponse.reservations()) {
      for (var instance : reservation.instances()) {
        if (instance.privateDnsName().equals(nodeName)) {
          StartInstancesRequest startInstancesRequest = StartInstancesRequest.builder()
              .instanceIds(instance.instanceId())
              .build();
          ec2Client.startInstances(startInstancesRequest);

          break;
        }

      }
    }
  }

  @Override
  public String getInstanceState(String nodeName) {
    DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().build();
    DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest);

    for (var reservation : describeInstancesResponse.reservations()) {
      for (var instance : reservation.instances()) {
        if (instance.privateDnsName().equals(nodeName)) {
          return instance.state().name().toString();
        }

      }
    }
    return null;
  }

  @Override
  public Boolean isInstanceStopped(String nodeName) {
    DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().build();
    DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest);

    for (var reservation : describeInstancesResponse.reservations()) {
      for (var instance : reservation.instances()) {
        if (instance.privateDnsName().equals(nodeName)) {
          return instance.state().name().toString().equals("stopped");
        }

      }
    }
    return null;
  }

  @Override
  public Boolean isRunning(String nodeName) {
    DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().build();
    DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest);

    for (var reservation : describeInstancesResponse.reservations()) {
      for (var instance : reservation.instances()) {
        if (instance.privateDnsName().equals(nodeName)) {
          if (instance.state().name().toString().equals("running")) {
            System.out.println("Healthy.");
            return true;
          }
        }

      }
    }
    return false;
  }
}
