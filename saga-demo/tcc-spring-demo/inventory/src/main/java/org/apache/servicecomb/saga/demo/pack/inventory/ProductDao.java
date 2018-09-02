package org.apache.servicecomb.saga.demo.pack.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductDao extends JpaRepository<Product, Long> {

}
