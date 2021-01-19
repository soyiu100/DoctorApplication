package com.doctorapp.configuration;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.doctorapp.constant.DoctorApplicationConstant.REGION;

@Configuration
public class DynamoDBConfiguration {

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(REGION)
                .build();
    }

    @Bean
    public DynamoDBMapper getDynamoDBMapper() {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDB();
        return new DynamoDBMapper(amazonDynamoDB);
    }
}