package org.mse802.sdk;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

/** Local regression test for Quokka 307 POST redirect handling.
 * Author: Shuohui Liu
 * Date: 28 Oct 2026
 */
public final class QuokkaRedirectTest {
  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/redirect", exchange -> {
      exchange.getRequestBody().readAllBytes();
      exchange.getResponseHeaders().set("Location", "/target");
      exchange.sendResponseHeaders(307, -1);
      exchange.close();
    });
    server.createContext("/target", exchange -> {
      String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      if (!exchange.getRequestMethod().equals("POST") || !body.contains("\"count\":3") ||
          !body.contains("\"script\":")) {
        exchange.sendResponseHeaders(400, -1);
      } else {
        byte[] response = "{\"result\":{\"c\":[0,1,0]}}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
      }
      exchange.close();
    });
    server.start();
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/redirect";
      var request = java.net.http.HttpRequest.newBuilder(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"count\":3,\"script\":\"test\"}"))
          .build();
      var response = Main.HTTP.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200 || !response.body().contains("\"result\"")) {
        throw new AssertionError("HTTP redirect regression: " + response.statusCode());
      }
      System.out.println("PASS: 307 redirect preserves POST payload");
    } finally {
      server.stop(0);
    }
  }
}
