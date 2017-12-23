package io.servicecomb.saga.omega.transport;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import io.servicecomb.saga.core.IdGenerator;

public class TransactionClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

  public static String TRANSACTION_ID_KEY = "X-Transaction-Id";

  private IdGenerator<String> randomIdGenerator = new UniqueIdGenerator();

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    if (!request.getHeaders().containsKey(TRANSACTION_ID_KEY)) {
      String txId = randomIdGenerator.nextId();
      request.getHeaders().add(TRANSACTION_ID_KEY, txId);
    }
    return execution.execute(request, body);
  }
}
