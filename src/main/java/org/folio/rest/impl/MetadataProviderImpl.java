package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.LogCollection;
import org.folio.rest.jaxrs.resource.MetadataProvider;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.services.JobExecutionService;
import org.folio.services.JobExecutionServiceImpl;
import org.folio.services.LogService;
import org.folio.services.LogServiceImpl;
import org.folio.services.converters.JobExecutionToDtoConverter;
import org.folio.util.ExceptionHelper;

import javax.ws.rs.core.Response;
import java.util.Map;

public class MetadataProviderImpl implements MetadataProvider {

  private final Logger logger = LoggerFactory.getLogger("mod-source-record-manager");

  private JobExecutionService jobExecutionService;
  private LogService logService;

  private JobExecutionToDtoConverter jobExecutionToDtoConverter;

  public MetadataProviderImpl(Vertx vertx, String tenantId) {
    String calculatedTenantId = TenantTool.calculateTenantId(tenantId);
    this.jobExecutionService = new JobExecutionServiceImpl(vertx, calculatedTenantId);
    this.logService = new LogServiceImpl(vertx);
    this.jobExecutionToDtoConverter = new JobExecutionToDtoConverter();
  }

  @Override
  public void getMetadataProviderLogs(String query, int offset, int limit, boolean landingPage,
                                      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                      Context vertxContext) {
    vertxContext.runOnContext(v -> {
      try {
        logService.getByQuery(query, offset, limit)
          .map(logs -> new LogCollection()
            .withLogs(logs)
            .withTotalRecords(logs.size()))
          .map(GetMetadataProviderLogsResponse::respond200WithApplicationJson)
          .map(Response.class::cast)
          .otherwise(ExceptionHelper::mapExceptionToResponse)
          .setHandler(asyncResultHandler);
      } catch (Exception e) {
        asyncResultHandler.handle(Future.succeededFuture(
          ExceptionHelper.mapExceptionToResponse(e)));
      }
    });
  }

  ;

  @Override
  public void getMetadataProviderJobExecutions(String query, int offset, int limit, Map<String, String> okapiHeaders,
                                               Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    vertxContext.runOnContext(v -> {
      try {
        jobExecutionService.getCollectionDtoByQuery(query, offset, limit)
          .map(GetMetadataProviderJobExecutionsResponse::respond200WithApplicationJson)
          .map(Response.class::cast)
          .otherwise(ExceptionHelper::mapExceptionToResponse)
          .setHandler(asyncResultHandler);
      } catch (Exception e) {
        asyncResultHandler.handle(Future.succeededFuture(
          ExceptionHelper.mapExceptionToResponse(e)));
      }
    });
  }
}
