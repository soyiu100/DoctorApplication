/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License
 * http://aws.amazon.com/asl/
 */
package com.doctorapp;

import com.doctorapp.constant.RoleEnum;
import com.doctorapp.dto.OAuthClientDetails;
import com.doctorapp.dto.OAuthPartner;
import com.google.common.collect.ImmutableList;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SampleDataLoader {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public void loadSampleData() {
        OAuthClientDetails testAlexaClient =
            OAuthClientDetails.builder()
                .clientId("new_alexa_client")
                .clientSecret(passwordEncoder.encode("test_client_secret"))
                .scopes("profile")
                .webServerRedirectUri("https://pitangui.amazon.com/api/skill/link/M1XIM5AKLYJGMM")
                .accessTokenValidity(3600)
                .refreshTokenValidity(0)
                .authorizedGrantTypes("implicit,authorization_code,refresh_token")
                .build();

        OAuthClientDetails adminClient =
            OAuthClientDetails.builder()
                .clientId("new_admin_client")
                .clientSecret(passwordEncoder.encode("test_client_secret"))
                .scopes("test_scope")
                .webServerRedirectUri("http://localhost:5000/redirect")
                .accessTokenValidity(3600)
                .refreshTokenValidity(0)
                .authorities(RoleEnum.ROLE_CLIENT_ADMIN.name())
                .authorizedGrantTypes("client_credentials,implicit,authorization_code,password,refresh_token")
                .build();

        OAuthPartner testAlexaPartner =
            OAuthPartner.builder()
                .partnerId("new_alexa_client")
                .clientId("amzn1.application-oa2-client.59a161140bbb49c8aa5c04bbff262db5")
                .clientSecret("ba40f89db1f7b991e09b276ea81c2c25b93d0e81523f570351bef2410b77a6aa")
                .scopes("alexa::health:profile:write")
                .accessTokenUri("https://api.amazon.com/auth/o2/token")
                .userAuthorizationUri("https://www.amazon.com/ap/oa")
                .preEstablishedRedirectUri("")
                .build();

        dynamoDBMapper.batchSave(
            ImmutableList.of(testAlexaClient, adminClient, testAlexaPartner));
    }
}
