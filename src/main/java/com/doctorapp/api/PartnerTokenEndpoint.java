/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License
 * http://aws.amazon.com/asl/
 */

package com.doctorapp.api;

import com.doctorapp.authentication.UserIDAuthenticationToken;
import com.doctorapp.dao.DynamoDBPartnerTokenDAO;
import com.doctorapp.dto.OAuthPartner;
import com.doctorapp.dao.DynamoDBPartnerDetailsDAO;
import java.util.Date;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest Controller for partner token endpoint.
 *
 * <p>
 * This endpoint is called by admin clients to retrieve access tokens received from partner OAuth providers (e.g. LWA).
 * <p>
 *
 * @author Lucun Cai
 */
@RestController
@Log4j2
public class PartnerTokenEndpoint {

    @Autowired
    private DynamoDBPartnerTokenDAO partnerTokenService;

    @Autowired
    private DynamoDBPartnerDetailsDAO partnerDetailsService;

    /**
     * Endpoint to retrieve a client token from ClientTokenService.
     */
    @RequestMapping(value = "/api/partner/token")
    public OAuth2AccessToken getPartnerToken(final @RequestParam Map<String, String> parameters) {
        final String userID = parameters.get("user_id");
        final String partnerId = parameters.get("partner_id");

        OAuthPartner partner = partnerDetailsService.loadPartnerByPartnerId(partnerId);

        if (partner == null) {
            throw new InvalidClientException("Invalid partner id: " + partnerId);
        }

        OAuth2ProtectedResourceDetails resourceDetails = partner.toProtectedResourceDetails();

        OAuth2AccessToken accessToken = partnerTokenService.getAccessToken(resourceDetails,
            new UserIDAuthenticationToken(userID));

        log.info(String.format("Retrieving partner token for userId: %s, partnerId: %s. Token: %s",
            userID, partnerId, accessToken));

        if (accessToken == null) {
            throw new OAuth2Exception("No token found for user: " + userID);
        } else if (accessToken.getExpiration().compareTo(new Date()) < 0) {
            log.info("Token has expired, refresh the token");
            //Token expired, refresh the token.
            accessToken = refreshClientToken(accessToken, resourceDetails);
        }

        partnerTokenService.saveAccessToken(resourceDetails, new UserIDAuthenticationToken(userID), accessToken);

        return accessToken;
    }

    /**
     * Refresh a client access token.
     */
    private OAuth2AccessToken refreshClientToken(final OAuth2AccessToken accessToken,
                                                 final OAuth2ProtectedResourceDetails resourceDetails) {
        final AccessTokenRequest AccessTokenRequest = new DefaultAccessTokenRequest();

        final AuthorizationCodeAccessTokenProvider tokenProvider = new AuthorizationCodeAccessTokenProvider();
        tokenProvider.setStateMandatory(false);

        return tokenProvider.refreshAccessToken(resourceDetails,
            accessToken.getRefreshToken(), AccessTokenRequest);
    }

}