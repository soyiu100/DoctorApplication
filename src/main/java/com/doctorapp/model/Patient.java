package com.doctorapp.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;


/**
 * A DTO object represents a Patient.
 * Usually, the account in Cognito and the DDB entry would be created through the auth server,
 * but for now there are dummy entries for testing purposes.
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "Patients")
public class Patient {

    @DynamoDBIgnore
    String username;

    @DynamoDBHashKey
    String patientId;

    @DynamoDBIgnore
    String emailAddress;

    String firstName;
    String lastName;
    String dob;

}
