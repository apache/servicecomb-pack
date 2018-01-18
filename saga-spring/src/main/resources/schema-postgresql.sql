CREATE TABLE IF NOT EXISTS SagaEventEntity (
  id BIGSERIAL PRIMARY KEY,
  sagaId varchar(36) NOT NULL,
  creationTime timestamp(6) NOT NULL DEFAULT CURRENT_DATE,
  type varchar(50) NOT NULL,
  contentJson TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS running_sagas_index ON SagaEventEntity (sagaId, type);
