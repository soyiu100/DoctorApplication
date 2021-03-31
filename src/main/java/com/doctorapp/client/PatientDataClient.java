package com.doctorapp.client;

import static com.doctorapp.constant.AWSConfigConstants.USERNAME;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.cognitoidp.model.UserType;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import com.doctorapp.dao.DynamoDBTokenDAO;
import com.doctorapp.dao.ScheduledSessionDao;
import com.doctorapp.data.ScheduledSession;
import com.doctorapp.dto.OAuthAccessToken;
import com.doctorapp.exception.DependencyException;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * In this demo, the Auth server and skill handler will be running on the same host. Instead of making
 * extra HTTP call to the same host, this client extracts out common code to be used by both components.
 */
@Log4j2
public class PatientDataClient {

    @Autowired
    DynamoDBTokenDAO dynamoDBTokenDAO;

    @Autowired
    CognitoClient cognitoClient;

    @Autowired
    ScheduledSessionDao scheduledSessionDao;

    public ScheduledSession putScheduledSession(ScheduledSession session) {
        return scheduledSessionDao.putScheduledSession(session);
    }

    public String getPatientIdWithAccessToken(String accessToken) {
        try {
            String username = getUserNameWithAccessToken(accessToken);
            return getPatientIdByUsername(username);
        } catch (DynamoDBMappingException e) {
            String errorMessage = String.format("Failed to get OAuthAccessTokens in DynamoDB using access token %s", accessToken);
            log.error(errorMessage, e);
            throw new DependencyException(errorMessage, e);
        }
    }

    public String getUserNameWithAccessToken(String accessToken) {
        try {
            List<OAuthAccessToken> targetUsers = dynamoDBTokenDAO.getOAuthAccessTokenUsingAccessToken(accessToken);
            // ...assuming access tokens are unique UUIDs!
            assert (targetUsers.size() == 1);
            return targetUsers.get(0).getUserName();
        } catch (DynamoDBMappingException e) {
            String errorMessage = String.format("Failed to get OAuthAccessTokens in DynamoDB using access token %s", accessToken);
            log.error(errorMessage, e);
            throw new DependencyException(errorMessage, e);
        }
    }

    public List<ScheduledSession> getSessionByPatientId(String patientId) {
        try {
            return scheduledSessionDao.getScheduledSessionsByPatientId(patientId);
        } catch (Exception e) {
            log.info("Error occurred while getting sessions by patient ID: {}", e.getMessage());
            throw e;
        }
    }

    public List<ScheduledSession> getSessionByUsername(String username) {
        try {
            String patientId = getPatientIdByUsername(username);

            return scheduledSessionDao.getScheduledSessionsByPatientId(patientId);
        } catch (Exception e) {
            log.info("Error occurred while getting sessions by patient ID: {}", e.getMessage());
            throw e;
        }
    }

    public Optional<ScheduledSession> getCurrentSessionsByPatientId(String patientId) {
        return scheduledSessionDao.getCurrentSessionsByPatientId(patientId);
    }

    private String getPatientIdByUsername(String username) {
        ListUsersResult userResult = cognitoClient.getPatientIdsByFilter(USERNAME, username.trim());
        List<UserType> patientUserType = userResult.getUsers();
        assert (patientUserType.size() == 1);

        List<AttributeType> attributes = patientUserType.get(0).getAttributes();

        // Only attribute should be patient ID
        assert (attributes.size() == 1);
        return attributes.get(0).getValue();

    }
}
