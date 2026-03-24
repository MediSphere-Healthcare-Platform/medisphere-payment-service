package com.medisphere.payment.filter;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Collectors;

@Component
@Log4j2
@RequiredArgsConstructor
public class RequestFilter implements Filter {

    private final Gson gson;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        MultipleReadHttpRequest wrappedRequest = new MultipleReadHttpRequest(request);
        BufferedServletResponseWrapper wrappedResponse = new BufferedServletResponseWrapper(response);

        traceRequest(wrappedRequest);

        chain.doFilter(wrappedRequest, wrappedResponse);

        traceResponse(wrappedResponse);

        byte[] responseBytes = wrappedResponse.getResponseData().getBytes();
        OutputStream os = response.getOutputStream();
        os.write(responseBytes);
    }

    private void traceRequest(HttpServletRequest request) throws IOException {
        log.info("---------------------> REQUEST BEGIN: {} {}", request.getMethod(), request.getRequestURI());

        if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                sb.append(reader.lines().collect(Collectors.joining(System.lineSeparator())));
            } catch (Exception e) {
                log.warn("Could not read request body for logging: {}", e.getMessage());
            }
            String body = sb.toString();
            if (!body.isEmpty()) {
                try {
                    log.info("Body: {}", gson.toJson(JsonParser.parseString(body)));
                } catch (Exception e) {
                    log.info("Body: {}", body);
                }
            }
        }
        log.info("--------------------> REQUEST END");
    }

    private void traceResponse(BufferedServletResponseWrapper response) {
        log.info("<-------------------- RESPONSE BEGIN: {}", response.getStatus());
        String data = response.getResponseData();
        if (data != null && !data.trim().isEmpty()) {
            try {
                log.info("Body: {}", gson.toJson(JsonParser.parseString(data)));
            } catch (Exception e) {
                log.info("Body: {}", data);
            }
        } else {
            log.info("Body: [empty]");
        }
        log.info("<-------------------- RESPONSE END");
    }
}
