package iudx.apd.acl.server.response;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.apiserver.response.ResponseUtil;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestResponseUtil {

  @Test
  @DisplayName("Test generate response method")
  public void testGenerateResponse(VertxTestContext vertxTestContext)
  {
    JsonObject actualResponse = ResponseUtil.generateResponse(HttpStatusCode.INTERNAL_SERVER_ERROR, ResponseUrn.DB_ERROR_URN);
    assertEquals(ResponseUrn.DB_ERROR_URN.getUrn(), actualResponse.getString(TYPE));
    assertEquals(ResponseUrn.INTERNAL_SERVER_ERROR.getMessage(), actualResponse.getString(TITLE));
    assertEquals(ResponseUrn.INTERNAL_SERVER_ERROR.getMessage(), actualResponse.getString(DETAIL));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test generateResponse with null status code")
  public void testGenerateResponseWithNullStatusCode(VertxTestContext vertxTestContext)
  {
    assertThrows(NullPointerException.class,
            ()->ResponseUtil.generateResponse(null, ResponseUrn.DB_ERROR_URN, "Some message"));
    vertxTestContext.completeNow();
  }
}
