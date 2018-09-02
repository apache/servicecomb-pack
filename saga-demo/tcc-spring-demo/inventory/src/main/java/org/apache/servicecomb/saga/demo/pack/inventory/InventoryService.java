package org.apache.servicecomb.saga.demo.pack.inventory;

import java.util.Objects;
import org.apache.servicecomb.saga.omega.transaction.annotations.Participate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

  private ProductDao productDao;

  /**
   * Find the product with specific product Id.
   *
   * @param productId Product ID
   * @param requiredCount Required product count
   * @return return the reserved count, 0 if nothing is available.
   */
  @Participate(confirmMethod = "confirm", cancelMethod = "cancel")
  public Integer reserve(Long productId, int requiredCount) {
    Product product = productDao.findOne(productId);
    if (Objects.isNull(product)) {
      throw new IllegalArgumentException("Product not exists at all");
    }

    // if it is sufficient
    if (product.getAvailable() > requiredCount) {
      product.setReserved(product.getReserved() + requiredCount); // reserve some product in stock
      productDao.save(product);
      return requiredCount;
    } else {
      return product.getAvailable() - requiredCount;
    }
  }

  public void confirm(Long productId, int requiredCount) {
    Product product = productDao.findOne(productId);
    product.setInStock(product.getInStock() - requiredCount); // actually reduce the in stock count
    product.setReserved(product.getReserved() - requiredCount); // and remove the reserved count
    productDao.save(product);
  }

  public void cancel(Long productId, int requiredCount) {
    Product product = productDao.findOne(productId);
    product.setReserved(product.getReserved() - requiredCount); // remove the reserved count
    productDao.save(product);
  }

  @Autowired
  public void setProductDao(ProductDao productDao) {
    this.productDao = productDao;
  }
}
