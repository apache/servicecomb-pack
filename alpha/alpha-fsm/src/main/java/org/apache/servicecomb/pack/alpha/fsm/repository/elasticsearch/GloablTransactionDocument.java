package org.apache.servicecomb.pack.alpha.fsm.repository.elasticsearch;

import org.apache.servicecomb.pack.alpha.fsm.repository.model.GlobalTransaction;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = ElasticsearchTransactionRepository.INDEX_NAME, type = ElasticsearchTransactionRepository.INDEX_TYPE)
public class GloablTransactionDocument extends GlobalTransaction {

}
