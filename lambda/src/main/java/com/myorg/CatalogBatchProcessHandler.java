package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.List;

public class CatalogBatchProcessHandler implements RequestHandler<SQSEvent, Void> {

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        List<SQSEvent.SQSMessage> messages = event.getRecords();

        for (SQSEvent.SQSMessage message : messages) {
            String body = message.getBody();

            // Parse JSON to Product model
            // Save to DynamoDB or your DB of choice
            context.getLogger().log("Creating product: " + body);
        }

        return null;
    }
}

