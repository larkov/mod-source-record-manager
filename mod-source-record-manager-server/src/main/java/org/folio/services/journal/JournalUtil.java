package org.folio.services.journal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.DataImportEventPayload;
import org.folio.rest.jaxrs.model.DataImportEventTypes;
import org.folio.rest.jaxrs.model.JournalRecord;
import org.folio.rest.jaxrs.model.Record;

import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isAnyEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.rest.jaxrs.model.DataImportEventTypes.DI_ERROR;
import static org.folio.rest.jaxrs.model.JournalRecord.EntityType.HOLDINGS;
import static org.folio.rest.jaxrs.model.JournalRecord.EntityType.INSTANCE;
import static org.folio.rest.jaxrs.model.JournalRecord.EntityType.ITEM;
import static org.folio.rest.jaxrs.model.JournalRecord.EntityType.MARC_AUTHORITY;
import static org.folio.rest.jaxrs.model.JournalRecord.EntityType.MARC_BIBLIOGRAPHIC;
import static org.folio.rest.jaxrs.model.JournalRecord.EntityType.MARC_HOLDINGS;

/**
 * Journal util class for building specific 'JournalRecord'-objects, based on parameters.
 */
public class JournalUtil {

  public static final Logger LOGGER = LogManager.getLogger();

  public static final String ERROR_KEY = "ERROR";
  private static final String EVENT_HAS_NO_DATA_MSG = "Failed to handle %s event, because event payload context does not contain %s and/or %s data";
  private static final String ENTITY_OR_RECORD_MAPPING_EXCEPTION_MSG = "Can`t map 'RECORD' or/and '%s'";

  private JournalUtil() {

  }

  public static JournalRecord buildJournalRecordByEvent(DataImportEventPayload event, JournalRecord.ActionType actionType,
                                                        JournalRecord.EntityType entityType, JournalRecord.ActionStatus actionStatus) throws JournalRecordMapperException {
    var context = event.getContext();
    String entityAsString = context.get(entityType.value());
    String recordAsString = extractRecord(context);

    if (INSTANCE.equals(entityType)) {
      if (isAnyEmpty(entityAsString, recordAsString)) {
        throw new JournalRecordMapperException(String.format(EVENT_HAS_NO_DATA_MSG, event.getEventType(), INSTANCE.value(), MARC_BIBLIOGRAPHIC.value()));
      }
    }

    return buildJournalRecord(event, actionType, entityType, actionStatus);
  }

  private static String extractRecord(HashMap<String, String> context) {
    return Optional.ofNullable(context.get(MARC_BIBLIOGRAPHIC.value()))
      .or(() -> Optional.ofNullable(context.get(MARC_AUTHORITY.value())))
      .or(() -> Optional.ofNullable(context.get(MARC_HOLDINGS.value())))
      .orElse(EMPTY);
  }

  public static JournalRecord buildJournalRecord(DataImportEventPayload eventPayload, JournalRecord.ActionType actionType, JournalRecord.EntityType entityType,
                                                 JournalRecord.ActionStatus actionStatus) throws JournalRecordMapperException {
    try {
      HashMap<String, String> eventPayloadContext = eventPayload.getContext();
      LOGGER.debug("buildJournalRecord: eventPayloadContext.size = {}, actionType = {}, entityType = {}, actionStatus = {}",
        eventPayloadContext.size(), actionType, entityType, actionStatus);

      String recordAsString = extractRecord(eventPayloadContext);
      LOGGER.debug("buildJournalRecord: recordAsString.length = {}", recordAsString.length());

      Record record = new ObjectMapper().readValue(recordAsString, Record.class);
      String entityAsString = eventPayloadContext.get(entityType.value());

      if (actionStatus == JournalRecord.ActionStatus.ERROR) {
        LOGGER.debug("buildJournalRecord: ERROR message: {} for actionStatus ERROR", eventPayloadContext.get(ERROR_KEY));
      }

      JournalRecord journalRecord = new JournalRecord()
        .withJobExecutionId(record.getSnapshotId())
        .withSourceId(record.getId())
        .withSourceRecordOrder(record.getOrder())
        .withEntityType(entityType)
        .withActionType(actionType)
        .withActionDate(new Date())
        .withActionStatus(actionStatus);

      LOGGER.debug("JournalUtil.buildJournalRecord: {}", 1);

      if (!isEmpty(entityAsString)) {
        LOGGER.debug("JournalUtil.buildJournalRecord: {}", 2);
        JsonObject entityJson = new JsonObject(entityAsString);
        LOGGER.debug("JournalUtil.buildJournalRecord: {}", 3);
        journalRecord.setEntityId(entityJson.getString("id"));
        LOGGER.debug("JournalUtil.buildJournalRecord: {}", 4);
        if (entityType == INSTANCE || entityType == HOLDINGS || entityType == ITEM) {
          LOGGER.debug("JournalUtil.buildJournalRecord: {}", 9);
          if (entityType == HOLDINGS) {
            LOGGER.debug("JournalUtil.buildJournalRecord: {}", 5);
            journalRecord.setInstanceId(entityJson.getString("instanceId"));
          }
          if (entityType == ITEM) {
            if (eventPayloadContext.containsKey(INSTANCE.value())) {
              JsonObject instanceJson = new JsonObject(eventPayloadContext.get(INSTANCE.value()));
              journalRecord.setInstanceId(instanceJson.getString("id"));
            } else if (eventPayloadContext.containsKey(HOLDINGS.value())) {
              JsonObject holdingsJson = new JsonObject(eventPayloadContext.get(HOLDINGS.value()));
              journalRecord.setInstanceId(holdingsJson.getString("instanceId"));
            }
            journalRecord.setHoldingsId(entityJson.getString("holdingsRecordId"));
          }
          LOGGER.debug("JournalUtil.buildJournalRecord: {}", 6);
          journalRecord.setEntityHrId(entityJson.getString("hrid"));
        }
      }

      LOGGER.debug("JournalUtil.buildJournalRecord: {}", 7);
      if (DI_ERROR == DataImportEventTypes.fromValue(eventPayload.getEventType())) {
        LOGGER.debug("JournalUtil.buildJournalRecord: {}", 8);
        journalRecord.setError(eventPayloadContext.get(ERROR_KEY));
      }

      LOGGER.debug("JournalUtil.buildJournalRecord: {}", 9);
      return journalRecord;
    } catch (Exception e) {
      throw new JournalRecordMapperException(String.format(ENTITY_OR_RECORD_MAPPING_EXCEPTION_MSG, entityType.value()), e);
    }
  }
}
