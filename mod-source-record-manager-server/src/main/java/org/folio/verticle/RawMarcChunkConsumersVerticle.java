package org.folio.verticle;

import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.ProcessRecordErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collections;
import java.util.List;

import static org.folio.rest.jaxrs.model.DataImportEventTypes.DI_RAW_RECORDS_CHUNK_READ;

public class RawMarcChunkConsumersVerticle extends AbstractConsumersVerticle {

  @Autowired
  @Qualifier("RawMarcChunksKafkaHandler")
  private AsyncRecordHandler<String, String> rawMarcChunksKafkaHandler;

  @Autowired
  @Qualifier("RawMarcChunksErrorHandler")
  private ProcessRecordErrorHandler<String, String> errorHandler;

  @Override
  public List<String> getEvents() {
    return Collections.singletonList(DI_RAW_RECORDS_CHUNK_READ.value());
  }

  @Override
  public AsyncRecordHandler<String, String> getHandler() {
    return this.rawMarcChunksKafkaHandler;
  }

  @Override
  public ProcessRecordErrorHandler<String, String> getErrorHandler() {
    return this.errorHandler;
  }

}
