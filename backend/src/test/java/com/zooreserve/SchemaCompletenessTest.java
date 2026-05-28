package com.zooreserve;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaCompletenessTest {

  @Test
  void schemaContainsCoreBusinessTablesAndIndexes() throws IOException {
    String schema = new String(getClass().getResourceAsStream("/db/schema.sql").readAllBytes(), StandardCharsets.UTF_8);

    assertThat(schema).contains(
        "CREATE TABLE IF NOT EXISTS user",
        "CREATE TABLE IF NOT EXISTS admin_user",
        "CREATE TABLE IF NOT EXISTS role",
        "CREATE TABLE IF NOT EXISTS permission",
        "CREATE TABLE IF NOT EXISTS ticket_inventory",
        "CREATE TABLE IF NOT EXISTS reservation_order",
        "CREATE TABLE IF NOT EXISTS payment_record",
        "CREATE TABLE IF NOT EXISTS refund_record",
        "CREATE TABLE IF NOT EXISTS checkin_record",
        "CREATE TABLE IF NOT EXISTS operation_log",
        "UNIQUE KEY uk_inventory",
        "UNIQUE KEY uk_activity_user"
    );
  }
}
