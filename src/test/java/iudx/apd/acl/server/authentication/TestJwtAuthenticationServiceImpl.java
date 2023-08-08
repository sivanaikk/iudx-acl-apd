package iudx.apd.acl.server.authentication;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestJwtAuthenticationServiceImpl {
    private static final Logger LOGGER = LogManager.getLogger(TestJwtAuthenticationServiceImpl.class);
    @Mock
    JsonObject jsonObject;
    private JwtAuthenticationServiceImpl jwtAuth;

    @BeforeEach
    public void init(VertxTestContext vertxTestContext)
    {
        jwtAuth = new JwtAuthenticationServiceImpl();
        vertxTestContext.completeNow();
    }

    @Test
    public void testTokenIntrospect(VertxTestContext vertxTestContext)
    {
        jwtAuth.tokenIntrospect(jsonObject,jsonObject,handler -> {
            if(handler.succeeded())
            {
                LOGGER.info("Success");
            }
            else {
                LOGGER.error("failed");
            }
        });
        vertxTestContext.completeNow();
    }
}