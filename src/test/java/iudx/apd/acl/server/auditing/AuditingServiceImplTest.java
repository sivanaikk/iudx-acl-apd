package iudx.apd.acl.server.auditing;

import static iudx.apd.acl.server.apiserver.util.Constants.EPOCH_TIME;
import static iudx.apd.acl.server.apiserver.util.Constants.ISO_TIME;
import static iudx.apd.acl.server.apiserver.util.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.auditing.databroker.DataBrokerService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class AuditingServiceImplTest {

  private static AuditingServiceImpl auditingService;
  private static DataBrokerService databroker;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
    databroker = mock(DataBrokerService.class);
    auditingService = new AuditingServiceImpl(databroker);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Testing Write Query Successful")
  void writeDataSuccessful(VertxTestContext vertxTestContext) {
    DataBrokerService dataBrokerService = mock(DataBrokerService.class);
    JsonObject request = new JsonObject();
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    long time = zst.toInstant().toEpochMilli();
    String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
    request.put(EPOCH_TIME, time);
    request.put(ISO_TIME, isoTime);
    request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    AuditingServiceImpl auditingService = new AuditingServiceImpl(dataBrokerService);

    when(dataBrokerService.publishMessage(anyString(), anyString(), any()))
        .thenReturn(Future.succeededFuture());

    auditingService
        .insertAuditlogIntoRmq(request)
        .onSuccess(
            successHandler -> {
              verify(dataBrokerService, times(1)).publishMessage(anyString(), anyString(), any());
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });

  }

  @Test
  @DisplayName("Testing Write Query Failure")
  void writeDataSuccessful2(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    long time = zst.toInstant().toEpochMilli();
    String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
    request.put(EPOCH_TIME, time);
    request.put(ISO_TIME, isoTime);
    request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    auditingService = new AuditingServiceImpl(databroker);

    when(databroker.publishMessage(anyString(), anyString(), any()))
        .thenReturn(Future.failedFuture("failed"));

    auditingService
        .insertAuditlogIntoRmq(request)
        .onFailure(f ->
        {
          assertEquals(f.getMessage(), "failed");
          vertxTestContext.completeNow();
        });
  }
}
