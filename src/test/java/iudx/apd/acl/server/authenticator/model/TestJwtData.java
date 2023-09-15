package iudx.apd.acl.server.authenticator.model;

import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.authentication.model.JwtData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestJwtData {
    JwtData jwtData;
    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext)
    {
      jwtData = new JwtData();
      jwtData.setSub("fd47486b-3497-4248-ac1e-082e4d37a66c");
      jwtData.setIss("authvertx.iudx.io");
      jwtData.setAud("rs.iudx.io");
      jwtData.setExp(1886135512);
      jwtData.setIat(1686135512);
      jwtData.setIid("rs:rs.iudx.io");
      jwtData.setRole("consumer");
      jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api").add("sub")));
      vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test toJson method")
    public void test_toJson(VertxTestContext vertxTestContext)
    {
      JsonObject expectedJson = new JsonObject();
      expectedJson.put("aud","rs.iudx.io");
      expectedJson.put("cons",new JsonObject().put("access", new JsonArray().add("api").add("sub")));
      expectedJson.put("exp",1886135512);
      expectedJson.put("iat",1686135512);
      expectedJson.put("iid","rs:rs.iudx.io");
      expectedJson.put("iss","authvertx.iudx.io");
      expectedJson.put("role","consumer");
      expectedJson.put("sub","fd47486b-3497-4248-ac1e-082e4d37a66c");
        JsonObject actual = jwtData.toJson();
        assertNotNull(actual);
        assertEquals(expectedJson,actual);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test getIat method")
    public void test_getIat(VertxTestContext vertxTestContext)
    {
        String actual = jwtData.getAud();
        assertEquals("rs.iudx.io",actual);
        vertxTestContext.completeNow();
    }
}
