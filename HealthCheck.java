import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

public class HealthCheck {

  public static void main(String[] args) throws InterruptedException, IOException {

    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/ping"))
        // production redirects plain HTTP unless the proxy header says HTTPS
        .header("X-Forwarded-Proto", "https")
        .build();

    var response = client.send(request, BodyHandlers.ofString());

    if (response.statusCode() != 200 || !response.body().contains("pong")) {
      throw new RuntimeException("Healthcheck failed");
    }
  }
}