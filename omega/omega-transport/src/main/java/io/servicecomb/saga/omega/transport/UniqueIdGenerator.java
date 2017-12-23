package io.servicecomb.saga.omega.transport;

import java.util.UUID;

import io.servicecomb.saga.core.IdGenerator;

public class UniqueIdGenerator implements IdGenerator<String> {
  @Override
  public String nextId() {
    return UUID.randomUUID().toString();
  }
}
