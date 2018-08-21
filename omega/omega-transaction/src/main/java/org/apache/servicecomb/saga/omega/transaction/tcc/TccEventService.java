package org.apache.servicecomb.saga.omega.transaction.tcc;

import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.ParticipatedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccEndedEvent;
import org.apache.servicecomb.saga.omega.transaction.tcc.events.TccStartedEvent;

public interface TccEventService {

  void onConnected();

  void onDisconnected();

  void close();

  String target();

  AlphaResponse participate(ParticipatedEvent participateEvent);

  AlphaResponse TccTransactionStart(TccStartedEvent tccStartEvent);

  AlphaResponse TccTransactionStop(TccEndedEvent tccEndEvent);

  AlphaResponse send(TxEvent event);
  
}
