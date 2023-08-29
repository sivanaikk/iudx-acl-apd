package iudx.apd.acl.server.metering;

import static iudx.apd.acl.server.apiserver.util.Constants.EPOCH_TIME;
import static iudx.apd.acl.server.apiserver.util.Constants.ISO_TIME;
import static iudx.apd.acl.server.apiserver.util.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.*;
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
import iudx.apd.acl.server.metering.databroker.DataBrokerService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class MeteringServiceImplTest {

  private static MeteringServiceImpl meteringService;
  private static DataBrokerService databroker;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
    databroker = mock(DataBrokerService.class);
    meteringService = new MeteringServiceImpl(databroker);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Testing Write Query Successful")
  void writeDataSuccessful(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    long time = zst.toInstant().toEpochMilli();
    String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
    request.put(EPOCH_TIME, time);
    request.put(ISO_TIME, isoTime);
    request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    meteringService = new MeteringServiceImpl(databroker);

    when(databroker.publishMessage(anyString(), anyString(), any()))
        .thenReturn(Future.succeededFuture());

    meteringService
        .insertMeteringValuesInRmq(request)
        .onSuccess(
            successHandler -> {
              vertxTestContext.completeNow();
            })
        .onFailure(
            failure -> {
              vertxTestContext.failNow(failure.getMessage());
            });
    verify(databroker, times(1)).publishMessage(anyString(), anyString(), any());
  }
}
