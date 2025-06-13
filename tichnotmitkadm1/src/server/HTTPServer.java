package server;


import servlets.Servlet;
/**
 * HTTPServer defines the minimal interface for an HTTP server implementation.
 * Any implementing class must be able to start listening on its port
 * and be closed gracefully.
 */

public interface HTTPServer extends Runnable{
    public void addServlet(String httpCommanmd, String uri, Servlet s);
    public void removeServlet(String httpCommanmd, String uri);
    public void start();
    public void close();
}
