package com.doctorapp.configuration;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.doctorapp.model.ScheduledSession;
import com.doctorapp.exception.DependencyException;
import com.doctorapp.model.TimeRange;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.doctorapp.constant.DoctorApplicationConstant.*;

@Log4j2
@Repository
public class ScheduledSessionDao {

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
     * Both updates and creates sessions.
     * @param scheduledSession The scheduled session.
     * @return The scheduled session.
     */
    public ScheduledSession putScheduledSession(@NonNull ScheduledSession scheduledSession) {
        try {
            dynamoDBMapper.save(scheduledSession);
            return scheduledSession;
        } catch (DynamoDBMappingException e) {
            String errorMessage = String.format("Failed to put record in DynamoDB for patientId %s",
                    scheduledSession.getPatientId());
            log.error(errorMessage, e);
            throw new DependencyException(errorMessage, e);
        }
    }

    /**
     * Get a list of scheduledSessions by Time Range;
     *
     *  @param timeRange the search time Range
     */
    public List<ScheduledSession> getScheduledSessionsByTimeRange(@NonNull TimeRange timeRange)  {
        try {
            Map<String, AttributeValue> expectedAttributes = new HashMap<>();
            if(StringUtils.isBlank(timeRange.getStartTime()) || StringUtils.isBlank(timeRange.getEndTime())) {
                throw new IllegalArgumentException("Invalid Input: StartTime or EndTime cannot be null" );
            }
            expectedAttributes.put(":startTime", new AttributeValue().withS(timeRange.getStartTime()));
            expectedAttributes.put(":endTime", new AttributeValue().withS(timeRange.getEndTime()));

            final DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                    .withFilterExpression(FILTER_EXPRESSION)
                    .withExpressionAttributeValues(expectedAttributes);

            return dynamoDBMapper.scan(ScheduledSession.class, scanExpression);
        } catch (DynamoDBMappingException e) {
            String errorMessage = String.format("Failed to get scheduledSessions in DynamoDB for input time range, " +
                    "startTime: %s, endTime: %s", timeRange.getStartTime(), timeRange.getEndTime());
            log.error(errorMessage, e);
            throw new DependencyException(errorMessage, e);
        }
    }

    /**
     * Get a list of scheduledSessions by patientId;
     *
     *  @param patientId the patientId
     */
    public List<ScheduledSession> getScheduledSessionsByPatientId(@NonNull String patientId)  {
        try {
            final DynamoDBQueryExpression<ScheduledSession> queryExpression =
                    new DynamoDBQueryExpression<ScheduledSession>()
                            .withHashKeyValues(ScheduledSession.builder().patientId(patientId).build())
                            .withScanIndexForward(false)
                            .withConsistentRead(true);
            return dynamoDBMapper
                    .query(ScheduledSession.class, queryExpression);

        } catch (DynamoDBMappingException e) {
            String errorMessage = String.format("Failed to get scheduledSessions in DynamoDB for patient %s", patientId);
            log.error(errorMessage, e);
            throw new DependencyException(errorMessage, e);
        }
    }

    /**
     * Get a list of scheduledSessions by patientId;
     *
     *  @param roomId the patientId
     */
    public ScheduledSession getScheduledSessionByRoomId(@NonNull String roomId)  {
        try {
            HashMap<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
            eav.put(":v_roomId", new AttributeValue().withS(roomId));
            final DynamoDBQueryExpression<ScheduledSession> queryExpression =
                    new DynamoDBQueryExpression<ScheduledSession>()
                            .withIndexName("roomId-index").withConsistentRead(false)
                            .withScanIndexForward(false)
                            .withKeyConditionExpression("roomId = :v_roomId")
                            .withExpressionAttributeValues(eav);
            List<ScheduledSession> targetSessionList =  dynamoDBMapper
                    .query(ScheduledSession.class, queryExpression);

            assert(targetSessionList.size() <= 1);

            if (targetSessionList.size() == 1) {
                return targetSessionList.get(0);
            } else {
                return null;
            }
        } catch (DynamoDBMappingException e) {
            String errorMessage = String.format("Failed to get scheduledSessions in DynamoDB for room ID %s", roomId);
            log.error(errorMessage, e);
            throw new DependencyException(errorMessage, e);
        }
    }

}
