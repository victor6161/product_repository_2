package com.myorg;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateProduct implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDB dynamoDB = new DynamoDB(client);
    private static final Table productsTable = dynamoDB.getTable("products");
    private static final Table stocksTable = dynamoDB.getTable("stocks");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(getCorsHeaders());

        try {
            JsonNode requestBody = objectMapper.readTree(apiGatewayProxyRequestEvent.getBody());

            String productId = UUID.randomUUID().toString();
            String title = requestBody.get("title").asText();
            String description = requestBody.get("description").asText();
            int price = requestBody.get("price").asInt();
            int count = requestBody.get("count").asInt();

            productsTable.putItem(new Item()
                    .withPrimaryKey("id", productId)
                    .withString("title", title)
                    .withString("description", description)
                    .withInt("price", price));

            stocksTable.putItem(new Item()
                    .withPrimaryKey("product_id", productId)
                    .withInt("count", count));

            response.setStatusCode(201);
            response.setBody("Product created successfully");
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("Internal Server Error: " + e.getMessage());
        }

        return response;
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
