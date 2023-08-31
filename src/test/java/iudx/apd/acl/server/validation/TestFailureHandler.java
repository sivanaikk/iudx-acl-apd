package iudx.apd.acl.server.validation;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.validation.exceptions.DxRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static iudx.apd.acl.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.apd.acl.server.apiserver.util.Constants.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestFailureHandler {

FailureHandler failureHandler;
@Mock
    RoutingContext routingContext;
@Mock Throwable throwable;
@Mock DxRuntimeException dxRuntimeException;
@Mock ResponseUrn responseUrn;
@Mock
    HttpServerResponse response;
@Mock
    Future<Void> voidFuture;
@Mock RuntimeException runtimeException;

    @BeforeEach
    public void init(VertxTestContext vertxTestContext){

        failureHandler = new FailureHandler();
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handle method with DxRunTimeException")
    public void testHandle(VertxTestContext vertxTestContext)
    {
        when(routingContext.failure()).thenReturn(dxRuntimeException);
        when(dxRuntimeException.getUrn()).thenReturn(responseUrn);
        when(responseUrn.getUrn()).thenReturn(ResponseUrn.DB_ERROR_URN.getUrn());
        when(dxRuntimeException.getMessage()).thenReturn("Some error message");
        when(dxRuntimeException.getStatusCode()).thenReturn(500);
        when(routingContext.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(response.end(anyString())).thenReturn(voidFuture);
        failureHandler.handle(routingContext);

        verify(routingContext,atLeast(2)).response();
        verify(routingContext, atLeast(1)).failure();
        verify(routingContext, atLeast(1)).next();
        verify(dxRuntimeException, atLeast(1)).getUrn();
        verify(responseUrn, atLeast(1)).getUrn();
        verify(response, atLeast(1)).setStatusCode(anyInt());
        verify(response, atLeast(1)).putHeader(anyString(), anyString());
        verify(response, atLeast(1)).setStatusCode(anyInt());
        verify(response, atLeast(1)).end(anyString());

        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test handle method with RuntimeException")
    public void testHandleWithRuntimeException(VertxTestContext vertxTestContext)
    {
        when(routingContext.failure()).thenReturn(runtimeException);
        when(routingContext.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(response.end(anyString())).thenReturn(voidFuture);
        when(response.ended()).thenReturn(true);
        failureHandler.handle(routingContext);

        verify(routingContext,atLeast(2)).response();
        verify(routingContext, atLeast(1)).failure();
        verify(response, atLeast(1)).putHeader(anyString(), anyString());
        verify(response, atLeast(1)).setStatusCode(anyInt());
        verify(response, atLeast(1)).end(anyString());
        verify(response, atLeast(1)).ended();
        vertxTestContext.completeNow();

    }



}
