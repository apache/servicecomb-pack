package org.apache.servicecomb.saga.demo.pack.inventory;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "t_product")
public class Product implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String name;

  /**
   * Total count in stock
   */
  private Integer inStock;

  /**
   * reserved count
   */
  private Integer reserved;

  Product() {
  }

  Product(String name, Integer inStock, Integer reserved) {
    this.name = name;
    this.inStock = inStock;
    this.reserved = reserved;
  }

  /**
   * Available in stock
   */
  @Formula("in_Stock - reserved")
  private Integer available;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getInStock() {
    return inStock;
  }

  public void setInStock(Integer inStock) {
    this.inStock = inStock;
  }

  public Integer getReserved() {
    return reserved;
  }

  public void setReserved(Integer reserved) {
    this.reserved = reserved;
  }

  public Integer getAvailable() {
    return available;
  }

  public void setAvailable(Integer available) {
    this.available = available;
  }
}
