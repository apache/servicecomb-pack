package org.apache.servicecomb.saga.demo.pack.inventory;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/product")
public class ProductController {

  private InventoryService inventoryService;

  @PostMapping("/product")
  public Response updateInventory(@RequestParam("productId") Long productId,
      @RequestParam("requiredCount") Integer requiredCount) {
    return new Response(0, "OK",
        ImmutableList.of(inventoryService.reserve(productId, requiredCount)));
  }

  @ExceptionHandler
  public Response exceptionHandler(Exception e) {
    return new Response(-1, e.getMessage(),
        ImmutableList.of(e.getStackTrace()));
  }

  @Autowired
  public void setInventoryService(
      InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }
}

class Response {

  private int code;

  private String message;

  private List<Object> objects;

  Response(int code, String message, List<Object> objects) {
    this.code = code;
    this.message = message;
    this.objects = objects;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<Object> getObjects() {
    return objects;
  }

  public void setObjects(List<Object> objects) {
    this.objects = objects;
  }
}
