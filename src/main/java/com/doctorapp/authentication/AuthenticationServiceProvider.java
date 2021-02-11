/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License
 * http://aws.amazon.com/asl/
 */
package com.doctorapp.authentication;

import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.doctorapp.client.CognitoClient;
import com.doctorapp.constant.DoctorApplicationConstant;
import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
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

        log.info("start verify username {}, password {} ", username, password);
        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", username);
        authParams.put("PASSWORD", password);

        String userType = request.getParameter("userType");
        log.info(userType);
        userType.trim();

        log.info("Start calling cognito");

        try {
            AdminInitiateAuthResult authResult = cognitoClient.getAuthResult(DoctorApplicationConstant.DOCTOR_POOL_ID, DoctorApplicationConstant.DOCTOR_POOL_CLIENT_ID, authParams);
            if (authResult.getChallengeName() != null &&
                    authResult.getChallengeName().equals("NEW_PASSWORD_REQUIRED")) {
                return new UsernamePasswordAuthenticationToken(username, password,
                        ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.UNVERIFIED_USER.name())));
            } else {
                log.info("steppin");
                return new UsernamePasswordAuthenticationToken(username, password,
                        ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.ROLE_DOCTOR.name())));
            }
        } catch (Exception e) {
            log.error("Failed to login" + e.getMessage(), e);
            //todo: Error message like : failed to validate your user credential
//            redirect.addFlashAttribute("error", true);
        }

//        if(authResult.getChallengeName().equals("NEW_PASSWORD_REQUIRED")) {
//            //todo : redirect to change password page
//            return new UsernamePasswordAuthenticationToken(username, password,
//                    ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.UNVERIFIED_USER.name())));
//        }
//
//        if(StringUtils.equals(username, "admin")) {
//            return new UsernamePasswordAuthenticationToken(username, password,
//                    ImmutableList.of(new SimpleGrantedAuthority(RoleEnum.ROLE_USER_ADMIN.name())));
//        } else {
//            return new UsernamePasswordAuthenticationToken(username, password,
//                    ImmutableList.of());
//        }
        return new UsernamePasswordAuthenticationToken(username, password,
                    ImmutableList.of());
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {

        //TODO: Remove this code
        return mockUsers.stream()
            .filter(u -> u.getUsername().equals(username))
            .findAny()
            .map(u -> new User(u.getUsername(), u.getPassword(), u.getAuthorities()))
            .orElseThrow(() -> new UsernameNotFoundException("User " + username + " cannot be found"));
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return authentication.equals(
            UsernamePasswordAuthenticationToken.class);
    }

}
