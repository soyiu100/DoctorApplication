/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License
 * http://aws.amazon.com/asl/
 */
package com.doctorapp.authentication;

import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.doctorapp.client.CognitoClient;
import com.doctorapp.constant.AWSConfigConstants;
import com.doctorapp.constant.RoleEnum;
import com.google.common.collect.ImmutableList;

import static com.doctorapp.constant.UserTypeConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nimbusds.oauth2.sdk.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.HttpServletRequest;


/**
 * An customized AuthenticationProvider.
 *
 * <p>
 *     TODO: Replace the sample users in this class with the actual authentication server or user DB.
 * </p>
 *
 * @author Lucun Cai
 */
@RequiredArgsConstructor
@Log4j2
public class AuthenticationServiceProvider implements AuthenticationProvider, AuthenticationManager,
    UserDetailsService {

    @Autowired(required = false)
    private HttpServletRequest request;

    @Autowired
    CognitoClient cognitoClient;

    private static final List<User> mockUsers = ImmutableList.of(
        new User("user", "$2a$10$tNrknh3ZtTQ4IWq.P1KSaOwIar7ToOM1TjQTmuxGIIjYCJvy.55uS",
            ImmutableList.of()),
        new User("admin", "$2a$10$tNrknh3ZtTQ4IWq.P1KSaOwIar7ToOM1TjQTmuxGIIjYCJvy.55uS",
            ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.ROLE_USER_ADMIN.name()))));

    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        log.info("Received username {}, password {} to verify", username, password);
        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", username);
        authParams.put("PASSWORD", password);

        String userType = request.getParameter("userType");
        if (userType.equals("") || userType == null) {
            userType = NONE;
        }
        userType.trim();
        log.info("Determined user type: {}", userType);

        log.info("Calling Cognito");

        AdminInitiateAuthResult authResult;
        try {
            if (userType.equals(PATIENT)) {
                authResult = cognitoClient.getAuthResult(AWSConfigConstants.PATIENT_POOL_ID, AWSConfigConstants.PATIENT_POOL_CLIENT_ID, authParams);
                return new UsernamePasswordAuthenticationToken(username, password,
                        decideGrantedAuthorities(authResult, PATIENT));
            } else if (userType.equals(DOCTOR)) {
                authResult = cognitoClient.getAuthResult(AWSConfigConstants.DOCTOR_POOL_ID, AWSConfigConstants.DOCTOR_POOL_CLIENT_ID, authParams);
                return new UsernamePasswordAuthenticationToken(username, password,
                        decideGrantedAuthorities(authResult, DOCTOR));
            } else if (userType.equals(ADMIN)) {
                authResult = cognitoClient.getAuthResult(AWSConfigConstants.ADMIN_POOL_ID, AWSConfigConstants.ADMIN_POOL_CLIENT_ID, authParams);
                return new UsernamePasswordAuthenticationToken(username, password,
                        decideGrantedAuthorities(authResult, ADMIN));
            } else {
                log.info("Going into the fallback case for authentication");
                // represents an edge case where no user type is passed. this would be a bug
                try {
                    authResult = cognitoClient.getAuthResult(AWSConfigConstants.PATIENT_POOL_ID, AWSConfigConstants.PATIENT_POOL_CLIENT_ID, authParams);
                    return new UsernamePasswordAuthenticationToken(username, password,
                            decideGrantedAuthorities(authResult, PATIENT));
                } catch (Exception e) {
                    authResult = cognitoClient.getAuthResult(AWSConfigConstants.DOCTOR_POOL_ID, AWSConfigConstants.DOCTOR_POOL_CLIENT_ID, authParams);
                    return new UsernamePasswordAuthenticationToken(username, password,
                            decideGrantedAuthorities(authResult, DOCTOR));
                }
            }
        } catch (Exception e) {
            log.error("Failed to login: " + e.getMessage(), e);
            throw new BadCredentialsException(e.getMessage());
        }
    }

    private List<SimpleGrantedAuthority> decideGrantedAuthorities(AdminInitiateAuthResult authResult, String userType) {
        if (authResult != null && authResult.getChallengeName() != null &&
                authResult.getChallengeName().equals("NEW_PASSWORD_REQUIRED")) {
            if (userType.equals(ADMIN)) {
                return ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.UNVERIFIED_ADMIN.name()));
            } else if (userType.equals(DOCTOR)) {
                return ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.UNVERIFIED_DOCTOR.name()));
            } else {
                return ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.UNVERIFIED_PATIENT.name()));
            }
        } else {
            if (userType.equals(ADMIN)) {
                return ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.ROLE_USER_ADMIN.name()), new SimpleGrantedAuthority(RoleEnum.ROLE_CLIENT_ADMIN.name()));
            } else if (userType.equals(DOCTOR)) {
                log.info("Authenticated a doctor B)");
                return ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.ROLE_DOCTOR.name()));
            } else {
                return ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.ROLE_PATIENT.name()));
            }

        }
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return null;
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return authentication.equals(
            UsernamePasswordAuthenticationToken.class);
    }

}
