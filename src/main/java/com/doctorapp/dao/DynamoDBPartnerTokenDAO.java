/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License
 * http://aws.amazon.com/asl/
 */
package com.doctorapp.dao;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.doctorapp.dto.OAuthPartnerToken;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.ClientKeyGenerator;
import org.springframework.security.oauth2.client.token.ClientTokenServices;
import org.springframework.security.oauth2.client.token.DefaultClientKeyGenerator;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * A DAO to access {@link OAuthPartnerToken} in DynamoDB.
 *
 * @author Lucun Cai
 */
@Log4j2
public class DynamoDBPartnerTokenDAO implements ClientTokenServices {

    private DynamoDBMapper dynamoDBMapper;

    private ClientKeyGenerator keyGenerator;

    public DynamoDBPartnerTokenDAO(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.keyGenerator = new DefaultClientKeyGenerator();
    }

    /**
     * Get the {@link OAuth2AccessToken} of a protected resource for the {@link Authentication} provided.
     *
     * @param resource partner protected resource.
     * @param authentication user authentication.
     * @return oauth access token.
     */
    @Override
    public OAuth2AccessToken getAccessToken(OAuth2ProtectedResourceDetails resource, Authentication authentication) {
        String authenticationId = keyGenerator.extractKey(resource, authentication);
        List<OAuthPartnerToken> accessTokens = getOAuthPartnerTokensByAuthenticationId(authenticationId);

        return accessTokens.stream().findAny().map(OAuthPartnerToken::getToken).orElse(null);
    }

    public OAuthPartnerToken getOAuthPartnerToken(OAuth2ProtectedResourceDetails resource, Authentication authentication) {
        String authenticationId = keyGenerator.extractKey(resource, authentication);
        List<OAuthPartnerToken> accessTokens = getOAuthPartnerTokensByAuthenticationId(authenticationId);

        return accessTokens.get(0);
    }

    /**
     * Save the {@link OAuth2AccessToken} of a partner protected resource for the {@link Authentication} provided.
     *
     * @param resource partner protected resource.
     * @param authentication user authentication.
     * @param accessToken oauth access token.
     */
    @Override
    public void saveAccessToken(OAuth2ProtectedResourceDetails resource,
                                Authentication authentication,
                                OAuth2AccessToken accessToken) {
        String userName = authentication != null ? authentication.getName() : null;

        removeDuplicatePartnerTokens(resource.getClientId(), userName);


        // Calculate expiration date
        Calendar date = Calendar.getInstance();
        Date expirationDate = new Date(date.getTimeInMillis() + (accessToken.getExpiresIn() * 1000L));
        log.info("Token will expire in -s" + accessToken.getExpiresIn());
        log.info("Current time is: " + date.getTime());
        log.info("expirationDate is: " + expirationDate.getTime());
        OAuthPartnerToken oauthPartnerToken = OAuthPartnerToken.builder()
            .tokenId(accessToken.getValue())
            .token(accessToken)
            .authenticationId(keyGenerator.extractKey(resource, authentication))
            .userName(userName)
            .expirationDate(expirationDate)
            .clientId(resource.getClientId())
            .build();

        dynamoDBMapper.save(oauthPartnerToken);
    }

    /**
     * Remove the all the access token of the partner protected resource for the {@link Authentication} provided.
     *
     * @param resource partner protected resource.
     * @param authentication user authentication.
     */
    @Override
    public void removeAccessToken(OAuth2ProtectedResourceDetails resource, Authentication authentication) {
        String authenticationId = keyGenerator.extractKey(resource, authentication);
        List<OAuthPartnerToken> accessTokens = getOAuthPartnerTokensByAuthenticationId(authenticationId);

        dynamoDBMapper.batchDelete(accessTokens);
    }

    private void removeDuplicatePartnerTokens(String clientId, String username) {
        log.info("Searching for any duplicate tokens with client ID {} and username {}",
                clientId, username);
        DynamoDBQueryExpression query = new DynamoDBQueryExpression<OAuthPartnerToken>()
                .withIndexName("clientId-userName-index")
                .withConsistentRead(Boolean.FALSE)
                .withHashKeyValues(OAuthPartnerToken.builder()
                        .clientId(clientId)
                        .build());
        List<OAuthPartnerToken> partnerTokens = dynamoDBMapper.query(OAuthPartnerToken.class, query);

        if (partnerTokens.size() != 0) {
            for (OAuthPartnerToken partnerToken : partnerTokens) {
                if (partnerToken.getUserName().equals(username)) {
                    dynamoDBMapper.delete(partnerToken);
                }
            }
        }
    }

    private List<OAuthPartnerToken> getOAuthPartnerTokensByAuthenticationId(String authenticationId) {
        DynamoDBQueryExpression query = new DynamoDBQueryExpression<OAuthPartnerToken>()
            .withIndexName("authenticationId-index")
            .withConsistentRead(Boolean.FALSE)
            .withHashKeyValues(OAuthPartnerToken.builder()
                .authenticationId(authenticationId)
                .build());
        return dynamoDBMapper.query(OAuthPartnerToken.class, query);
    }

}
