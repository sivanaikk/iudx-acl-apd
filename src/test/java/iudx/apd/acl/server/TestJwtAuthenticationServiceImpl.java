package iudx.apd.acl.server;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.authentication.JwtAuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestJwtAuthenticationServiceImpl {
    private JwtAuthenticationServiceImpl jwtAuth;
    @Mock
    JsonObject jsonObject;


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
                System.out.println("Success");
            }
            else {
                System.out.println("failed");
            }
        });
        vertxTestContext.completeNow();
    }
}
