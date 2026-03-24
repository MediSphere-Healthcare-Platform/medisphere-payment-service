package com.medisphere.payment.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

@Log4j2
public class BufferedServletResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private PrintWriter writer;
    private int status = 200;

    public BufferedServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setStatus(int sc) {
        super.setStatus(sc);
        this.status = sc;
    }

    @Override
    public void sendError(int sc) throws IOException {
        super.sendError(sc);
        this.status = sc;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        super.sendError(sc, msg);
        this.status = sc;
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        super.sendRedirect(location);
        this.status = 302;
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int b) {
                baos.write(b);
            }
        };
    }

    @Override
    public PrintWriter getWriter() {
        if (writer == null) {
            try {
                writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
            } catch (UnsupportedEncodingException e) {
                log.error("Unsupported encoding: {}", getCharacterEncoding());
                writer = new PrintWriter(new OutputStreamWriter(getOutputStream()));
            }
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        super.flushBuffer();
    }

    public String getResponseData() {
        return baos.toString();
    }
}
