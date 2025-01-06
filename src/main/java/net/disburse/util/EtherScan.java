package net.disburse.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class EtherScan {
    public static final String BASE_URL = "https://api.etherscan.io/api";
    @Getter
    private static String apiKey;

    @Value("${etherscan.api.key}")
    public void setApiKey(String key) {
        apiKey = key;
    }

    public static boolean checkWalletAddress(String address) {
        try {
            String fullUrl = BASE_URL + "?module=account&action=balance&address=" + address + "&tag=latest&apikey=" + apiKey;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            EtherScanResponse apiResponse = mapper.readValue(response.body(), EtherScanResponse.class);

            return "1".equals(apiResponse.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Setter
    @Getter
    static class EtherScanResponse {
        private String status;
        private String message;
        private String result;

    }
}
