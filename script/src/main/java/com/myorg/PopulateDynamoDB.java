package com.myorg;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PopulateDynamoDB {

    private static final String PRODUCTS_TABLE = "products";
    private static final String STOCKS_TABLE = "stocks";

    public static void main(String[] args) {
        Region region = Region.EU_CENTRAL_1; // Change to your AWS region
        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        // Sample products data
        String[][] productsData = {
                {"Product RIKI TIKI TAVI", "RIKI TIKI TAVI", "100"},
                {"Product B", "Description for Product B", "150"},
                {"Product C", "Description for Product C", "200"}
        };

        for (String[] productData : productsData) {
            String productId = UUID.randomUUID().toString();
            createProduct(ddb, productId, productData[0], productData[1], Integer.parseInt(productData[2]));
            createStock(ddb, productId, 50);  // Default stock count of 50
        }

        ddb.close();
    }

    public static void createProduct(DynamoDbClient ddb, String id, String title, String description, int price) {
        Map<String, AttributeValue> productItem = new HashMap<>();
        productItem.put("id", AttributeValue.builder().s(id).build());
        productItem.put("title", AttributeValue.builder().s(title).build());
        productItem.put("description", AttributeValue.builder().s(description).build());
        productItem.put("price", AttributeValue.builder().n(String.valueOf(price)).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(PRODUCTS_TABLE)
                .item(productItem)
                .build();

        try {
            ddb.putItem(request);
            System.out.println("Inserted product: " + title);
        } catch (DynamoDbException e) {
            System.err.println("Error inserting product: " + title);
            System.err.println(e.getMessage());
        }
    }

    public static void createStock(DynamoDbClient ddb, String productId, int count) {
        Map<String, AttributeValue> stockItem = new HashMap<>();
        stockItem.put("product_id", AttributeValue.builder().s(productId).build());
        stockItem.put("count", AttributeValue.builder().n(String.valueOf(count)).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(STOCKS_TABLE)
                .item(stockItem)
                .build();

        try {
            ddb.putItem(request);
            System.out.println("Inserted stock for product_id: " + productId);
        } catch (DynamoDbException e) {
            System.err.println("Error inserting stock for product_id: " + productId);
            System.err.println(e.getMessage());
        }
    }
}

