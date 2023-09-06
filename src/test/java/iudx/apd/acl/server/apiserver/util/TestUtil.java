package iudx.apd.acl.server.apiserver.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.common.HttpStatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URISyntaxException;
import java.util.List;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestUtil {

  @Test
  public void toUriFunctionTest(VertxTestContext vertxTestContext) {
    assertEquals("some-URI", Util.toUriFunction.apply("some-URI").toString());
    vertxTestContext.completeNow();
  }

  @Test
  public void toListTest(VertxTestContext vertxTestContext) {
    JsonArray input = new JsonArray();
    input.add("some-element-1");
    input.add("some-element-2");
    input.add("some-element-3");

    List expected = List.of("some-element-1", "some-element-2", "some-element-3");
    assertEquals(expected, Util.toList(input));
    vertxTestContext.completeNow();
  }
  @Test
  public void toListTestWithNullInput(VertxTestContext vertxTestContext) {
    assertNull(Util.toList(null));
    vertxTestContext.completeNow();
  }

  @Test
  public void testErrorResponse(VertxTestContext vertxTestContext) {
    JsonObject expected =
        new JsonObject()
            .put(TYPE, "urn:dx:acl:notAcceptable")
            .put(TITLE, "Not Acceptable")
            .put(DETAIL, "Not Acceptable");
    assertEquals(expected.encode(), Util.errorResponse(HttpStatusCode.NOT_ACCEPTABLE));
    vertxTestContext.completeNow();
  }
}
