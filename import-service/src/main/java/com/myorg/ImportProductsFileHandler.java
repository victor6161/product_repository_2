package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ImportProductsFileHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        // Get filename from the request
        context.getLogger().log("!!!!!!!!!!!!!!!! ImportProductsFileHandler reached");

        Map<String, String> queryParams = request.getQueryStringParameters();
        context.getLogger().log("!!!!!!!!!!!!!!!! ImportProductsFileHandler reached");

        String fileName = queryParams.get("name");
        // Generate a pre-signed URL valid for 15 minutes
        Date expiration = Date.from(Instant.now().plus(Duration.ofMinutes(15)));
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest("rsschool-task-5", "uploaded/" + fileName)
                .withMethod(com.amazonaws.HttpMethod.PUT)
                .withExpiration(expiration);

        URL url = s3.generatePresignedUrl(generatePresignedUrlRequest);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody("{\"url\": \"" + url.toString() + "\"}")
                .withHeaders(getCorsHeaders());
    }

    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*"); // Разрешает все домены
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"); // Разрешенные методы
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        return headers;
    }
}