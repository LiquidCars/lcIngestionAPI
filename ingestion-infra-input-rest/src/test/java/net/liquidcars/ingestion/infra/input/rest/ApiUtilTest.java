package net.liquidcars.ingestion.infra.input.rest;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.NativeWebRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApiUtilTest {

    @Mock
    private NativeWebRequest request;

    @Mock
    private HttpServletResponse response;

    private StringWriter responseContent;


    @Test
    void shouldSetExampleResponseCorrectly() throws IOException {
        StringWriter responseContent = new StringWriter();
        PrintWriter writer = new PrintWriter(responseContent);

        when(request.getNativeResponse(HttpServletResponse.class)).thenReturn(response);
        when(response.getWriter()).thenReturn(writer);

        String contentType = "application/json";
        String exampleBody = "{\"status\": \"ok\"}";

        ApiUtil.setExampleResponse(request, contentType, exampleBody);

        verify(response).setCharacterEncoding("UTF-8");
        verify(response).addHeader("Content-Type", contentType);
        assertEquals(exampleBody, responseContent.toString());
    }

    @Test
    void shouldThrowRuntimeExceptionWhenIOExceptionOccurs() throws IOException {
        when(request.getNativeResponse(HttpServletResponse.class)).thenReturn(response);
        when(response.getWriter()).thenThrow(new IOException("Error"));

        assertThrows(RuntimeException.class, () -> {
            ApiUtil.setExampleResponse(request, "text/plain", "error");
        });
    }

    @Test
    void testConstructorIsPrivate() {
        ApiUtil util = new ApiUtil();
        assertNotNull(util);
    }
}
