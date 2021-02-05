package com.doctorapp.model;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel.DynamoDBAttributeType;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * A DTO object represents a ScheduledSession
 *
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "ScheduledSessions")
public class ScheduledSession {

    @DynamoDBHashKey
    private String patientId;

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "roomId-index", attributeName = "roomId")
    private String roomId;

    @DynamoDBRangeKey
    private String scheduledTime;

    private int durationInMin;

    @DynamoDBTyped(DynamoDBAttributeType.BOOL)
    private boolean doctorStatus;

    @DynamoDBTyped(DynamoDBAttributeType.BOOL)
    private boolean patientStatus;

    // Local fields; they don't go in DDB!!!!
    @DynamoDBIgnore
    private String date;
    @DynamoDBIgnore
    private String time;
    @DynamoDBIgnore
    private Patient patient;
}
