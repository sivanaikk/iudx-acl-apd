package iudx.apd.acl.server.exceptions;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.validation.exceptions.DxRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestDxRunTimeException {
    @Mock ResponseUrn responseUrn;
    DxRuntimeException dxRuntimeException;
    @Mock Throwable throwable;

    @Test
    @DisplayName("Test DxRuntimeException")
    public void testDxRuntimeExceptionWithoutMessage(VertxTestContext vertxTestContext){
        int statusCode = 400;
        dxRuntimeException = new DxRuntimeException(statusCode, responseUrn);
        verify(responseUrn, atLeast(1)).getMessage();
        assertEquals(400, dxRuntimeException.getStatusCode());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test DxRuntimeException")
    public void testDxRuntimeExceptionWithMessage(VertxTestContext vertxTestContext){
        int statusCode = 400;
        String errorMessage = "Something is wrong in the user's end";
        dxRuntimeException = new DxRuntimeException(statusCode, responseUrn, errorMessage);
        assertEquals(400, dxRuntimeException.getStatusCode());
        assertEquals("Something is wrong in the user's end", dxRuntimeException.getMessage());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test DxRuntimeException")
    public void testDxRuntimeExceptionWithError(VertxTestContext vertxTestContext){
        int statusCode = 500;
        String errorMessage = "Something is wrong in the server's end";
        when(responseUrn.getMessage()).thenReturn(errorMessage);
        dxRuntimeException = new DxRuntimeException(statusCode, responseUrn, throwable);
        assertEquals(500, dxRuntimeException.getStatusCode());
        verify(responseUrn, atLeast(1)).getMessage();
        assertEquals("Something is wrong in the server's end", dxRuntimeException.getMessage());
        vertxTestContext.completeNow();
    }
}
