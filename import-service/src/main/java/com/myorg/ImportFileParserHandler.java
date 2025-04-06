package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import com.opencsv.CSVReader;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ImportFileParserHandler implements RequestHandler<S3Event, String> {

    private final S3Client s3Client = S3Client.builder().region(Region.EU_CENTRAL_1).build();
    private final SqsClient sqsClient = SqsClient.builder().region(Region.EU_CENTRAL_1).build();
    private static final String QUEUE_NAME = "catalogItemsQueue";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        try {
            String bucket = s3Event.getRecords().get(0).getS3().getBucket().getName();
            String key = s3Event.getRecords().get(0).getS3().getObject().getKey();

            context.getLogger().log("Reading from bucket: " + bucket + ", key: " + key);

            ResponseInputStream<?> s3Object = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());

            CSVReader reader = new CSVReader(new InputStreamReader(s3Object));

            String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(QUEUE_NAME)
                    .build())
                    .queueUrl();
            context.getLogger().log("!!!QueueUrl: " + queueUrl);

            while (reader.readNext() != null) {
                Map<String, String> product = new HashMap<>();
                String [] line = reader.readNext();
                product.put("title", line[0]);
                product.put("description", line[1]);
                product.put("price", line[2]);
                product.put("quantity", line[3]);

                String messageBody = objectMapper.writeValueAsString(product);
                context.getLogger().log("!!!!MessageBody: " + messageBody);

                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
                        .build());

                context.getLogger().log("Sent to SQS: " + messageBody);
            }

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return "Error";
        }

        return "Done";
    }
}
