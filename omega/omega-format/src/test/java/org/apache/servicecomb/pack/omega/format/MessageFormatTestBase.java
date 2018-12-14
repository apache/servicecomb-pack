/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.pack.omega.format;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import java.util.Objects;

import org.apache.servicecomb.pack.omega.transaction.OmegaException;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class MessageFormatTestBase {

  static MessageFormat format;

  @Test
  public void serializeObjectIntoBytes() throws Exception {
    byte[] bytes = format.serialize(new Object[]{"hello", "world"});

    Object[] message = format.deserialize(bytes);

    assertThat(asList(message).containsAll(asList("hello", "world")), is(true));
  }

  @Test
  public void serializePOJOIntoBytes() {
    Order order = new Order();
    order.setOrderNumber("XXXXX001");
    order.setUnits(20);
    Product product = new Product();
    product.setName("ProductA");
    product.setPrice(2.2);
    order.setProduct(product);

    byte[] bytes = format.serialize(new Object[] {"order", order});
    Object[] message = format.deserialize(bytes);
    assertThat(asList(message).containsAll(asList("order", order)), is (true));

  }

  @Test
  public void serializeNullIntoBytes() throws Exception {
    byte[] bytes = format.serialize(null);

    Object[] message = format.deserialize(bytes);

    assertThat(message, is(nullValue()));
  }

  @Test
  public void blowsUpWhenObjectIsNotDeserializable() throws Exception {
    try {
      format.deserialize(new byte[0]);
      expectFailing(OmegaException.class);
    } catch (OmegaException e) {
      assertThat(e.getMessage(), startsWith("Unable to deserialize message"));
    }
  }

  static class EmptyClass {
  }

  static class Product {
    String name;
    double price;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public double getPrice() {
      return price;
    }

    public void setPrice(double price) {
      this.price = price;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Product product = (Product) o;
      return Double.compare(product.price, price) == 0 &&
          Objects.equals(name, product.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, price);
    }
  }

  static class Order {
    String orderNumber;
    int units;
    Product product;

    public Product getProduct() {
      return product;
    }

    public void setProduct(Product product) {
      this.product = product;
    }

    String getOrderNumber() {
      return orderNumber;
    }

    void setOrderNumber(String orderNumber) {
      this.orderNumber = orderNumber;
    }

    int getUnits() {
      return units;
    }

    void setUnits(int units) {
      this.units = units;
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Order order = (Order) o;
      return units == order.units &&
          Objects.equals(orderNumber, order.orderNumber) &&
          Objects.equals(product, order.product);
    }

    @Override
    public int hashCode() {
      return Objects.hash(orderNumber, units, product);
    }
  }


}
