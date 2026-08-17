package com.ziboto.backend.exception;

import com.ziboto.backend.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void missingFaviconReturnsNotFoundInsteadOfServerError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/favicon.ico");
        ResponseEntity<ApiResponse<Object>> response = handler.handleNoResourceFoundException(
                new NoResourceFoundException(HttpMethod.GET, "/favicon.ico", "No static resource favicon.ico"),
                new ServletWebRequest(request));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
