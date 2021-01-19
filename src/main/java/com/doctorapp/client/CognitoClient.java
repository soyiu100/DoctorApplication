package com.doctorapp.client;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;
import com.doctorapp.model.Doctor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

import static com.doctorapp.constant.DoctorApplicationConstant.*;

@Service
@Log4j2
public class CognitoClient {

    private AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    @PostConstruct
    public void init() {
        awsCognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(REGION)
                .build();

    }

    public AdminInitiateAuthResult getAuthResult(Map<String, String> authParams) {
        AdminInitiateAuthRequest authRequest = new AdminInitiateAuthRequest()
                .withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .withUserPoolId(DOCTOR_POOL_ID)
                .withClientId(DOCTOR_POOL_CLIENT_ID)
                .withAuthParameters(authParams);
        AdminInitiateAuthResult authResult = null;
        try {
            authResult = awsCognitoIdentityProvider.adminInitiateAuth(authRequest);
        } catch (Exception e) {
            throw new AWSCognitoIdentityProviderException("Failed to validate user");
        }
        return authResult;
    }


    public void changeFromTemporaryPassword(final Map<String, String> challengeResponses, @NonNull final String authSession)
            throws AWSCognitoIdentityProviderException {
        AdminRespondToAuthChallengeRequest changePasswordRequest = new AdminRespondToAuthChallengeRequest()
                .withChallengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                .withChallengeResponses(challengeResponses)
                .withClientId(DOCTOR_POOL_CLIENT_ID)
                .withUserPoolId(DOCTOR_POOL_ID)
                .withSession(authSession);

        try {
            log.info("start to change user password");
            awsCognitoIdentityProvider.adminRespondToAuthChallenge(changePasswordRequest);
            log.info("succeed to change user password");
        } catch (Exception e) {
            log.info("Error to change user password", e);
            throw new AWSCognitoIdentityProviderException("Error to change user password");
        }
    }


    public void createNewUser(final Doctor doctor) throws AWSCognitoIdentityProviderException {
        final String emailAddr = doctor.getEmailAddress();
        final String title = doctor.getTitle();
        final String firstName = doctor.getFirstName();
        final String lastName = doctor.getLastName();
        final String partnerName = doctor.getPartnerName();
        AdminCreateUserRequest createUserRequest = new AdminCreateUserRequest()
                .withUserPoolId(DOCTOR_POOL_ID)
                .withUsername(doctor.getUserName())
                .withUserAttributes(
                        new AttributeType()
                                .withName(EMAIL)
                                .withValue(emailAddr),
                        new AttributeType()
                                .withName("email_verified")
                                .withValue("true"),
                        new AttributeType()
                                .withName(TITLE)
                                .withValue(title),
                        new AttributeType()
                                .withName(FIRSTNAME)
                                .withValue(firstName),
                        new AttributeType()
                                .withName(LASTNAME)
                                .withValue(lastName),
                        new AttributeType()
                                .withName(PARTNERNAME)
                                .withValue(partnerName)
                );
        try {
            awsCognitoIdentityProvider.adminCreateUser(createUserRequest);
            log.info("succeed in creating doctor");
        } catch (Exception e) {
            log.info("Error in creating doctor", e);
            throw new AWSCognitoIdentityProviderException("Error in creating doctor");
        }
    }


    /**
     * Find users by input filters
     * @param paramMaps
     * @return a list of users
     */
    public ListUsersResult getUsersByFilter(Map<String, String> paramMaps)
            throws AWSCognitoIdentityProviderException {
        ListUsersRequest listUsersRequest = new ListUsersRequest()
                .withUserPoolId(DOCTOR_POOL_ID);
        paramMaps.forEach((filter, param) -> {
            String query = String.format(DOCTOR_FILTER_QUERY, filter, param);
            log.info("Start to fetch user list by query {}", query);
            listUsersRequest.setFilter(query);
        });

        ListUsersResult usersResults;
        try {
            usersResults = awsCognitoIdentityProvider.listUsers(listUsersRequest);
        } catch (Exception e) {
            log.error("Failed to fetch user lists", e);
            throw new AWSCognitoIdentityProviderException("Failed to fetch user lists");
        }
        return usersResults;
    }

}
