package org.apache.servicecomb.pack.alpha.spec.saga.db;

import org.apache.servicecomb.pack.alpha.spec.saga.db.test.AlphaEventController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfiguration implements WebMvcConfigurer {

  @Bean
  AlphaEventController alphaEventController(TxEventEnvelopeRepository eventRepository) {
    return new AlphaEventController(eventRepository);
  }

  @Bean
  SagaTransactionsController sagaTransactionsController(TxEventEnvelopeRepository eventRepository){
    return new SagaTransactionsController(eventRepository);
  }
}