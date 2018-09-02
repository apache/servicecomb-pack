package org.apache.servicecomb.saga.demo.pack.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class InventoryServiceTest {

  private InventoryService inventoryService;

  @Test
  public void reserve() {
    Integer count = inventoryService.reserve(1L, 10);
    assertThat(count).isNotNull().isEqualTo(10);
  }

  @Autowired
  public void setInventoryService(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }
}