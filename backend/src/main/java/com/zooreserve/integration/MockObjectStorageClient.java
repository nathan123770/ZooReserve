package com.zooreserve.integration;

import org.springframework.stereotype.Component;

@Component
public class MockObjectStorageClient implements ObjectStorageClient {

  @Override
  public String provider() {
    return "mock-storage";
  }
}
