package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ImportFileParserHandler implements RequestHandler<S3Event, String> {

    private final S3Client s3Client = S3Client.create();

    @Override
    public String handleRequest(S3Event event, Context context) {
        String bucket = event.getRecords().get(0).getS3().getBucket().getName();
        String key = event.getRecords().get(0).getS3().getObject().getKey();

        context.getLogger().log("Reading file from S3: " + bucket + "/" + key);

        try {
            ResponseInputStream<?> s3Object = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(s3Object, StandardCharsets.UTF_8));
                 CSVReader csvReader = new CSVReader(reader)) {

                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    context.getLogger().log("CSV Record: " + String.join(", ", line));
                }
            } catch (CsvValidationException e) {
                context.getLogger().log("CsvValidationException: "  + e.getMessage());
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            context.getLogger().log("Error reading or parsing file: " + e.getMessage());
        }

        return "Done";
    }
}
