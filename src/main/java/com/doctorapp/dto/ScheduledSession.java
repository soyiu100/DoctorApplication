package com.doctorapp.dto;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * An DTO object represents an ScheduledSessions
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

    private String roomId;

    @DynamoDBRangeKey
    private String scheduledTime;

    private int durationInMin;

    private String doctorStatus;

    private String patientStatus;
}
