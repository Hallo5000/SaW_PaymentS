package de.hallo5000.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class RESTendpoint {

    public void startHttpServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/api/createJob", new RESTCaller());

        server.setExecutor(null);//set Threadpool
        server.start();
    }

    private static class RESTCaller implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if("GET".equals(exchange.getRequestMethod())){String response = "Hello World";

                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");//set header (probably json in prod)

                // 3. Statuscode 200 (OK) und Länge der Antwort senden
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                // 4. Body schreiben und schließen
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
            exchange.sendResponseHeaders(406, -1);

        }
    }

}
