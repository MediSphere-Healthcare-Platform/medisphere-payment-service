package com.medisphere.payment.filter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
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
import java.util.Enumeration;

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

        // Skip non-API requestso
        if (request.getRequestURI().contains("actuator") || request.getRequestURI().contains("favicon")) {
            chain.doFilter(req, res);
            return;
        }

        MultipleReadHttpRequest wrappedRequest = new MultipleReadHttpRequest(request);
        BufferedServletResponseWrapper wrappedResponse = new BufferedServletResponseWrapper(response);

        traceRequest(wrappedRequest);

        chain.doFilter(wrappedRequest, wrappedResponse);

        traceResponse(wrappedResponse);

        // Copy content back to original response
        byte[] responseBytes = wrappedResponse.getResponseData().getBytes();
        OutputStream os = response.getOutputStream();
        os.write(responseBytes);
        os.flush();
        os.close();
    }

    private void traceRequest(HttpServletRequest request) throws IOException {
        log.info("==================== REQUEST BEGIN ====================");
        log.info("HTTP Method: {}", request.getMethod());
        log.info("Request URI: {}", request.getRequestURI());
        log.debug("Headers: {}", getHeaders(request));

        if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod())) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            log.info("Request Body: {}", sb.toString());
        }
        log.info("==================== REQUEST END ====================");
    }

    private void traceResponse(BufferedServletResponseWrapper response) {
        log.info("==================== RESPONSE BEGIN ====================");
        log.info("HTTP Status: {}", response.getStatus());
        String data = response.getResponseData();
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

        if (data != null && !data.trim().isEmpty()) {
            try {
                JsonElement jsonElement = JsonParser.parseString(data);
                log.info("Response Body:\n{}", prettyGson.toJson(jsonElement));
            } catch (Exception e) {
                log.info("Response Body: {}", data);
            }
        } else {
            log.info("Response Body: [empty]");
        }
        log.info("==================== RESPONSE END ====================");
    }

    private String getHeaders(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        StringBuilder sb = new StringBuilder("{");
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            sb.append(name).append(": ").append(request.getHeader(name)).append(", ");
        }
        if (sb.length() > 1)
            sb.setLength(sb.length() - 2);
        return sb.append("}").toString();
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
