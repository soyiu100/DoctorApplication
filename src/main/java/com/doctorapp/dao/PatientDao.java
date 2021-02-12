package com.doctorapp.client;


import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import com.doctorapp.constant.AWSConfigConstants;
import com.doctorapp.data.Patient;
import com.doctorapp.exception.DependencyException;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

@Log4j2
@Repository
public class PatientDao {
    private DynamoDBMapper dynamoDBMapper;

    @PostConstruct
    public void init() {
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(AWSConfigConstants.REGION)
                .build();
        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);
    }

    /**
     * Get patient by ID.
     * CRITICAL ASSUMPTION: every patient ID is unique
     *
     * @param id id
     */
    public Patient getPatientById(@NonNull String id) {
        return dynamoDBMapper.load(Patient.class, id);
    }


    /**
     * Both updates and creates sessions.
     *
     * @param patient The scheduled session.
     * @return The scheduled session.
     */
    public Patient putPatient(@NonNull Patient patient) {
        try {
            dynamoDBMapper.save(patient);
            return patient;
        } catch (DynamoDBMappingException e) {
            String errorMessage = String.format("Failed to put record in DynamoDB for patientId %s",
                    patient.getPatientId());
            log.error(errorMessage, e);
            throw new DependencyException(errorMessage, e);
        }
    }

}
