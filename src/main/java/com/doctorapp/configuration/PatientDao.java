package com.doctorapp.configuration;


import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.doctorapp.exception.DependencyException;
import com.doctorapp.model.Patient;
import com.doctorapp.model.ScheduledSession;
import com.doctorapp.model.TimeRange;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.doctorapp.constant.DoctorApplicationConstant.FILTER_EXPRESSION;
import static com.doctorapp.constant.DoctorApplicationConstant.REGION;

@Log4j2
@Repository
public class PatientDao {
    private DynamoDBMapper dynamoDBMapper;

    @PostConstruct
    public void init() {
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(REGION)
                .build();
        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);
    }

    /**
     * Get patient by ID.
     * CRITICAL ASSUMPTION: every patient ID is unique
     *
     *  @param id id
     */
    public Patient getPatientById(@NonNull String id)  {
        return dynamoDBMapper.load(Patient.class, id);
    }

}
