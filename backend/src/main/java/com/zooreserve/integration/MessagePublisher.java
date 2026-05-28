package com.zooreserve.integration;

public interface MessagePublisher {
  void publish(String topic, Object payload);
}
