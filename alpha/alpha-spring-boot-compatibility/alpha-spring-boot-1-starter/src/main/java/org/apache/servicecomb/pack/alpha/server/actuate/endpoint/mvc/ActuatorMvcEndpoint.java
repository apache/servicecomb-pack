package org.apache.servicecomb.pack.alpha.server.actuate.endpoint.mvc;

import org.apache.servicecomb.pack.alpha.server.actuate.endpoint.ActuatorEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

@Component
public class ActuatorMvcEndpoint extends EndpointMvcAdapter {

  private final ActuatorEndpoint delegate;

  @Autowired
  public ActuatorMvcEndpoint(ActuatorEndpoint delegate) {
    super(delegate);
    this.delegate = delegate;
  }

  @RequestMapping(value = "/{endpoint}", method = {RequestMethod.GET, RequestMethod.POST})
  @ResponseBody
  public Object endpoint(@PathVariable("endpoint") String id, @RequestParam(required = false) Boolean enabled,
                         @RequestParam(required = false) Boolean sensitive) {
    Predicate<Endpoint> isEnabled =
            endpoint -> matches(endpoint::isEnabled, ofNullable(enabled));

    Predicate<Endpoint> isSensitive =
            endpoint -> matches(endpoint::isSensitive, ofNullable(sensitive));

    Predicate<Endpoint> matchEndPoint =
            endpoint -> matches(endpoint::getId, ofNullable(id));

    return this.delegate.endpoints().stream()
            .filter(matchEndPoint.and(isEnabled.and(isSensitive)))
            .findAny().get().invoke();
  }

  private <T> boolean matches(Supplier<T> supplier, Optional<T> value) {
    return !value.isPresent() || supplier.get().equals(value.get());
  }
}
