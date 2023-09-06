package iudx.apd.acl.server.response;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.apiserver.response.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestRestResponse {
    RestResponse response;
    RestResponse.Builder builder;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext){
        builder = new RestResponse.Builder();
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test Builder class methods")
    public void testBuilderMethods(VertxTestContext vertxTestContext)
    {
        builder.withType("some-type");
        builder.withTitle("some-title");
        builder.withMessage("some-message");
        response = builder.build();
        JsonObject actualResponse = response.toJson();
        assertEquals("some-type", actualResponse.getString(TYPE));
        assertEquals("some-title", actualResponse.getString(TITLE));
        assertEquals("some-message", actualResponse.getString(DETAIL));
        assertNull(actualResponse.getString(STATUS_CODE));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test parameterized build method in Builder class")
    public void testBuildMethod(VertxTestContext vertxTestContext)
    {
        response = builder.build(200, "some-type", "some-title", "some-message");
        JsonObject actualResponse = response.toJson();
        assertEquals("some-type", actualResponse.getString(TYPE));
        assertEquals("some-title", actualResponse.getString(TITLE));
        assertEquals("some-message", actualResponse.getString(DETAIL));
        assertEquals(200, actualResponse.getInteger(STATUS_CODE));
        vertxTestContext.completeNow();
    }

}
