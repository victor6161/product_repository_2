package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ImportProductsFileHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String BUCKET_NAME_CDK = "rsschool-task-5-cdk";
    private final S3Presigner presigner = S3Presigner.builder()
            .region(Region.EU_CENTRAL_1) // Replace with your region
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        // Get filename from the request
        context.getLogger().log("!!!!!!!!!!!!!!!! ImportProductsFileHandler reached");

        Map<String, String> queryParams = request.getQueryStringParameters();
        context.getLogger().log("!!!!!!!!!!!!!!!! ImportProductsFileHandler reached");

        String fileName = queryParams.get("name");

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME_CDK)
                .key("uploaded/" + fileName)
                .contentType("text/csv")
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presign -> presign
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest));

        URL url = presignedRequest.url();

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