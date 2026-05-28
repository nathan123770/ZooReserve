package com.zooreserve.integration;

import org.springframework.stereotype.Component;

@Component
public class MockMessagePublisher implements MessagePublisher {

  @Override
  public void publish(String topic, Object payload) {
    // Mock boundary for RabbitMQ/RocketMQ integration in later milestones.
  }
}
