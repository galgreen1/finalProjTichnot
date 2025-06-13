package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import server.RequestParser.RequestInfo;
import servlets.Servlet;

/**
 * MyHTTPServer implements a simple reusable HTTP server library.
 * It listens on a specified port, accepts HTTP connections, parses requests,
 * and dispatches them to registered Servlets. The server runs in its own thread.

 */
public class MyHTTPServer extends Thread implements HTTPServer {
    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private volatile boolean running = true;
    private final Map<String, Servlet> servlets = new HashMap<>();

    /**
     * Constructs a new MyHTTPServer that will listen on the given port
     * and use a fixed-size thread pool to handle incoming connections.
     *
     * @param port     the TCP port to bind for incoming HTTP requests (1–65535)
     * @param nThreads the number of threads in the thread pool for request handling
     * @throws IllegalArgumentException if port is not between 1 and 65535,
     *                                  or if nThreads is less than 1
     */
    public MyHTTPServer(int port, int nThreads) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (nThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0");
        }

        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(nThreads);
    }

    /**
     * Registers a Servlet to handle HTTP requests matching the given method and URI.
     * The lookup key is formed as "METHOD URI" (for example, "GET /publish").
     *
     * @param httpCommand the HTTP method (e.g., "GET", "POST"), case-insensitive
     * @param uri         the request path (e.g., "/publish", "/app/resource")
     * @param s           the Servlet instance that implements {@link Servlet} interface
     * @throws NullPointerException if httpCommand, uri, or s is null
     */
    public void addServlet(String httpCommand, String uri, Servlet s) {
        if (httpCommand == null || uri == null || s == null) {
            throw new NullPointerException("httpCommand, uri, and servlet must not be null");
        }
        String key = httpCommand.toUpperCase() + " " + uri;
        servlets.put(key, s);
        System.out.println("Added servlet: " + key);
    }

    /**
     * Unregisters a previously registered Servlet for the given HTTP method and URI.
     *
     * @param httpCommand the HTTP method (e.g., "GET", "POST"), case-insensitive
     * @param uri         the request path to remove
     * @throws NullPointerException if httpCommand or uri is null
     */
    public void removeServlet(String httpCommand, String uri) {
        if (httpCommand == null || uri == null) {
            throw new NullPointerException("httpCommand and uri must not be null");
        }
        String key = httpCommand.toUpperCase() + " " + uri;
        servlets.remove(key);
        System.out.println("Removed servlet: " + key);
    }

    /**
     * Starts the HTTP server thread. This method creates a ServerSocket bound to the configured port,
     * then repeatedly accepts client connections and dispatches each to the thread pool for processing.
     * When {@link #close()} is called, the serverSocket is closed and the loop terminates gracefully.
     */
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);

            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    threadPool.execute(() -> handleClient(client));
                } catch (java.net.SocketException se) {
                    // Expected when serverSocket.close() is invoked during shutdown
                    if (!running) {
                        break;  // Normal shutdown
                    } else {
                        throw se;  // Unexpected socket exception
                    }
                }
            }
        } catch (IOException e) {
            // Handle errors constructing ServerSocket or during accept()
            e.printStackTrace();
        } finally {
            // Cleanup resources on exit
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {}
            }
            threadPool.shutdown();
        }
    }

    /**
     * Parses an incoming HTTP request, looks up the appropriate Servlet, and invokes its handle method.
     * If no matching Servlet is found, returns a 404 Not Found response.
     *
     * @param client the accepted client Socket from ServerSocket.accept()
     */
    private void handleClient(Socket client) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                OutputStream out = client.getOutputStream()
        ) {
            RequestInfo info = RequestParser.parseRequest(reader);
            if (info == null) {
                client.close();
                return;
            }

            String key = info.getHttpCommand().toUpperCase() + " " + info.getUri();
            Servlet servlet = findServlet(info.getHttpCommand(), info.getUri());

            if (servlet != null) {
                servlet.handle(info, out);
            } else {
                // Return HTTP 404 Not Found
                String resp = ""
                        + "HTTP/1.1 404 Not Found\r\n"
                        + "Content-Length: 0\r\n"
                        + "\r\n";
                out.write(resp.getBytes());
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Locates the appropriate Servlet for a given HTTP method and URI using two strategies:
     * <ol>
     *   <li>Exact match on "METHOD URI".</li>
     *   <li>Longest-prefix match on URI if no exact match is found.</li>
     * </ol>
     *
     * @param method the HTTP method (e.g., "GET", "POST"), case-insensitive
     * @param uri    the request URI (e.g., "/publish", "/upload/config")
     * @return the matching Servlet instance, or null if none is found
     */
    private Servlet findServlet(String method, String uri) {
        String upperMethod = method.toUpperCase();
        // Exact match
        String exactKey = upperMethod + " " + uri;
        Servlet best = servlets.get(exactKey);
        if (best != null) {
            return best;
        }

        // Longest-prefix match
        int bestLen = -1;
        for (Map.Entry<String, Servlet> entry : servlets.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(upperMethod + " ")) {
                continue;
            }
            String path = key.substring(upperMethod.length() + 1);
            boolean match = uri.startsWith(path) || uri.endsWith(path);
            if (match && path.length() > bestLen) {
                bestLen = path.length();
                best = entry.getValue();
            }
        }
        return best;
    }

    /**
     * Shuts down the HTTP server by setting running = false, closing the ServerSocket,
     * and shutting down the thread pool. After close() is called, the run() loop will exit.
     */
    @Override
    public void close() {
        running = false;
        threadPool.shutdown();

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
