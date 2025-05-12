package ch.uzh.ifi.hase.soprafs24.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionAdviceTest {

    private GlobalExceptionAdvice exceptionAdvice;
    private WebRequest webRequest;
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        exceptionAdvice = new GlobalExceptionAdvice();
        httpServletRequest = new MockHttpServletRequest("GET", "/test-url");
        webRequest = new ServletWebRequest(httpServletRequest);
    }

    @Test
    void handleConflict_withIllegalArgumentException() {
        
        IllegalArgumentException exception = new IllegalArgumentException("Test illegal argument");

        
        var response = exceptionAdvice.handleConflict(exception, webRequest);

        
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This should be application specific", response.getBody());
    }

    @Test
    void handleConflict_withIllegalStateException() {
        
        IllegalStateException exception = new IllegalStateException("Test illegal state");

        
        var response = exceptionAdvice.handleConflict(exception, webRequest);

        
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This should be application specific", response.getBody());
    }

    @Test
    void handleTransactionSystemException() {
        
        TransactionSystemException exception = new TransactionSystemException("Transaction failed");

        
        ResponseStatusException result = exceptionAdvice.handleTransactionSystemException(exception, httpServletRequest);

        
        assertNotNull(result);
        assertEquals(HttpStatus.CONFLICT, result.getStatus());
        assertTrue(result.getMessage().contains("Transaction failed"));
    }

    @Test
    void handleTransactionSystemException_withNestedCause() {
        
        IllegalStateException cause = new IllegalStateException("Validation failed");
        TransactionSystemException exception = new TransactionSystemException("Transaction failed", cause);

        
        ResponseStatusException result = exceptionAdvice.handleTransactionSystemException(exception, httpServletRequest);

        
        assertNotNull(result);
        assertEquals(HttpStatus.CONFLICT, result.getStatus());
        assertTrue(result.getMessage().contains("Transaction failed"));
        assertSame(exception, result.getCause());
    }

    @Test
    void handleException_internalServerError() {
        
        HttpServerErrorException exception = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                null,
                null,
                null
        );

        
        ResponseStatusException result = exceptionAdvice.handleException(exception);

        
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("Internal server error"));
        assertSame(exception, result.getCause());
    }

    @Test
    void handleException_withGenericException() {
        
        Exception exception = new Exception("Generic exception");

        
        ResponseStatusException result = exceptionAdvice.handleException(exception);

        
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("Generic exception"));
        assertSame(exception, result.getCause());
    }

    @Test
    void handleException_withRuntimeException() {
        
        RuntimeException exception = new RuntimeException("Runtime exception");

        
        ResponseStatusException result = exceptionAdvice.handleException(exception);

        
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatus());
        assertTrue(result.getMessage().contains("Runtime exception"));
        assertSame(exception, result.getCause());
    }

    @Test
    void handleException_withNullMessage() {
        
        Exception exception = new Exception();

        
        ResponseStatusException result = exceptionAdvice.handleException(exception);

        
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatus());

        
        
        assertNotNull(result.getMessage());
    }
}