package org.folio.services;

import static io.vertx.core.Future.succeededFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.Record;
import org.folio.dao.MappingRuleDao;

@RunWith(VertxUnitRunner.class)
public class MappingRuleServiceTest {

  protected static final String TEST_TENANT = "tenant";
  @Mock
  private MappingRuleDao mappingRuleDao;
  @Mock
  private MappingRuleCache mappingRuleCache;

  private MappingRuleServiceImpl ruleService;

  private AutoCloseable mocks;

  @Before
  public void setUp() throws IOException {
    mocks = MockitoAnnotations.openMocks(this);
    ruleService = new MappingRuleServiceImpl(mappingRuleDao, mappingRuleCache);
  }

  @After
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void testSaveDefaultRules(TestContext context) {
    when(mappingRuleDao.get(eq(Record.RecordType.MARC_BIB), any())).thenReturn(succeededFuture(Optional.empty()));
    when(mappingRuleDao.save(any(), eq(Record.RecordType.MARC_BIB), any())).thenReturn(succeededFuture("random-id"));

    Async async = context.async();
    var future = ruleService.saveDefaultRules(Record.RecordType.MARC_BIB, TEST_TENANT);

    verify(mappingRuleDao, times(1)).save(any(), eq(Record.RecordType.MARC_BIB), any());

    future.onComplete(ar -> {
      context.verify(v -> {
        Assert.assertTrue(ar.succeeded());
      });
      async.complete();
    });
  }

  @Test
  public void testSaveDefaultRulesIfRulesAlreadyExist(TestContext context) {
    when(mappingRuleDao.get(eq(Record.RecordType.MARC_BIB), any()))
      .thenReturn(succeededFuture(Optional.of(new JsonObject())));

    Async async = context.async();
    var future = ruleService.saveDefaultRules(Record.RecordType.MARC_BIB, TEST_TENANT);

    verify(mappingRuleDao, never()).save(any(), eq(Record.RecordType.MARC_BIB), any());

    future.onComplete(ar -> {
      context.verify(v -> {
        Assert.assertTrue(ar.succeeded());
      });
      async.complete();
    });
  }
}