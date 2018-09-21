package org.apache.servicecomb.saga.alpha.server;

import java.util.List;
import org.apache.servicecomb.saga.alpha.core.TxEventHistory;
import org.springframework.data.repository.CrudRepository;

public interface TxEventHistoryEnvelopeRepository extends CrudRepository<TxEventHistory,Long> {
  List<TxEventHistory> findByGlobalTxId(String globalTxId);
}
