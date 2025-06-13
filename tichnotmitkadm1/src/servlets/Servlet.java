package servlets;

import java.io.IOException;
import java.io.OutputStream;

import server.RequestParser.RequestInfo;
/**
 * Servlet is the interface that all request-handling classes must implement.
 * A Servlet processes a single HTTP request (wrapped in RequestInfo) and
 * writes the HTTP response to the provided OutputStream.
 */
public interface Servlet {
    void handle(RequestInfo ri, OutputStream toClient) throws IOException;
    void close() throws IOException;
}
