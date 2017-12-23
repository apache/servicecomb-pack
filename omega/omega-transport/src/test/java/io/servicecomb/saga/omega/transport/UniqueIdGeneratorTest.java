package io.servicecomb.saga.omega.transport;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class UniqueIdGeneratorTest {

  private UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  @Test
  public void nextIdIsUnique() {
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      String id = idGenerator.nextId();
      ids.add(id);
    }
    assertThat(ids.size(), is(10));
  }
}