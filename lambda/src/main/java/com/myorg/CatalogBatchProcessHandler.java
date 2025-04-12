package com.myorg;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class CatalogBatchProcessHandler implements RequestHandler<SQSEvent, Void> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDB dynamoDB = new DynamoDB(client);
    private static final Table productsTable = dynamoDB.getTable("products");


    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        List<SQSEvent.SQSMessage> messages = event.getRecords();

        for (SQSEvent.SQSMessage message : messages) {
            String body = message.getBody();

            // Parse JSON to Product model
            // Save to DynamoDB or your DB of choice
            context.getLogger().log("Creating product: " + body);

            // Deserialize message body into product model
            Product product;
            try {
                product = objectMapper.readValue(body, Product.class);
            } catch (JsonProcessingException e) {
                context.getLogger().log("Error parsing products message {}" + e.getMessage());
                throw new RuntimeException(e);
            }

            productsTable.putItem(new Item()
                    .withPrimaryKey("id", product.id())
                    .withString("title", product.title())
                    .withString("description", product.description())
                    .withInt("count", product.count())
                    .withInt("price", product.price()));

            context.getLogger().log("Saved product: " + product.title());
        }

        return null;
    }
}

