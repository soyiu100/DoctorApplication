package com.doctorapp.api;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.cognitoidp.model.UserType;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import com.doctorapp.client.CognitoClient;
import com.doctorapp.constant.AWSConfigConstants;
import com.doctorapp.dao.DynamoDBTokenDAO;
import com.doctorapp.dao.ScheduledSessionDao;
import com.doctorapp.data.ScheduledSession;
import com.doctorapp.dto.OAuthAccessToken;
import com.doctorapp.exception.DependencyException;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

import static com.doctorapp.constant.AWSConfigConstants.USERNAME;

@RestController
@Log4j2
public class PatientInfoEndpoint {

    @Autowired
    DynamoDBTokenDAO dynamoDBTokenDAO;

    @Autowired
    CognitoClient cognitoClient;

    @Autowired
    ScheduledSessionDao scheduledSessionDao;

    /**
     * Grabs the patient ID associated with the patient linked to the access token.
     *
     * @param accessToken
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/api/patients/accessToken", method = RequestMethod.POST)
    public String getPatientIdWithAccessToken(String accessToken) {
        try {
            List<OAuthAccessToken> targetUsers = dynamoDBTokenDAO.getOAuthAccessTokenUsingAccessToken(accessToken);

            // ...assuming access tokens are unique UUIDs!
            assert (targetUsers.size() == 1);

            String username = targetUsers.get(0).getUserName();

            return getPatientIdByUsername(username);
        } catch (DynamoDBMappingException e) {
            String errorMessage = String.format("Failed to get OAuthAccessTokens in DynamoDB using access token %s", accessToken);
            log.error(errorMessage, e);
            throw new DependencyException(errorMessage, e);
        }
    }

    /**
     * TODO: untested but implemented
     * As per documentation,
     * 1) 3P Skill Handler receives the username of the patient and invokes (POST?) to the webapp
     * 2) the webapp grabs the patient ID using the username
     * 3) then searches a DDB table to get the sessions
     *
     * Possible requirements:
     * - Valid CSRF token, as Spring Security is very picky about any POSTs that have a response body
     */
    @ResponseBody
    @RequestMapping(value = "/api/patients/username", method = RequestMethod.POST)
    public List<ScheduledSession> getSessionByUsername(String username) {
        try {
            String patientId = getPatientIdByUsername(username);

            return scheduledSessionDao.getScheduledSessionsByPatientId(patientId);
        } catch (Exception e) {
            log.info("Error occurred while getting sessions by patient ID: {}", e.getMessage());
            throw e;
        }
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
