package io.servicecomb.saga.core;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @Type(value = JsonTransaction.class, name = "rest")
})
public interface Transaction extends Operation {

  Transaction SAGA_START_TRANSACTION = () -> {
  };

  Transaction SAGA_END_TRANSACTION = () -> {
  };
}
