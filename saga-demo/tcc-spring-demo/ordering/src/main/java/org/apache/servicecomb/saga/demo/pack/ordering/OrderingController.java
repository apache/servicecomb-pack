package org.apache.servicecomb.saga.demo.pack.ordering;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/ordering")
public class OrderingController {

  @PostMapping("/{productId}")
  public String ordering(
      @RequestParam("requiredCount") Integer requiredCount) {
    // place an order

    // try to reserve some product

    // create a purchase order and do the payment

    // create delivery order

    return "";
  }
}
