package com.zooreserve.integration;

import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway implements PaymentGateway {

  @Override
  public String provider() {
    return "mock-payment";
  }
}
