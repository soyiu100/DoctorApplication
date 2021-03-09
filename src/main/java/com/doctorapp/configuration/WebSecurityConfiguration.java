/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License
 * http://aws.amazon.com/asl/
 */
package com.doctorapp.configuration;

import com.doctorapp.authentication.AuthenticationServiceProvider;
import com.doctorapp.authentication.CustomAccessDeniedHandler;
import com.doctorapp.authentication.FailureHandler;
import com.doctorapp.authentication.SuccessHandler;
import com.doctorapp.constant.RoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configuration for web security.
 *
 * @author Lucun Cai
 */
@EnableWebSecurity
@Configuration
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthenticationServiceProvider authenticationServiceProvider;

    @Autowired
    private SuccessHandler successHandler;

    @Autowired
    private FailureHandler failureHandler;


    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/webjars/**", "/resources/**",
                // put POST endpoints here that you want the config to ignore
                "/change_password_form",
                "/create_patient_form", "/create_doctor_form", "/create_admin_form",
                "/create_session_form",
                "/connect_session", "/disconnect_session",
                "/search_patient", "/alexa/telehealth/**", "/api/patients/accessToken");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .mvcMatchers("/login**", "/doctor/login", "/admin/login", "/logout.do",
                    "/css/**", "/js/**", "/images/**", "/actuator/**",
                    "/register", "/patient/register", "/admin/register",
                    "/change_password**", "/error", "/search_patient").permitAll()
            .mvcMatchers("/clients/**", "/partners/**").hasAuthority(RoleEnum.ROLE_USER_ADMIN.name())
            .mvcMatchers( "/create_session", "/search_patient", "/view_sessions",
                "/session_call", "/call", "/call/**", "/webjars/**").hasAuthority(RoleEnum.ROLE_DOCTOR.name())
            .anyRequest().authenticated()
            .and()
            .formLogin()
            .loginPage("/login")
            .usernameParameter("username")
            .passwordParameter("password")
            .successHandler(successHandler)
            .failureHandler(failureHandler)
            .loginProcessingUrl("/login.do")
            .and()
            .httpBasic()
            .and()
            .logout()
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout.do"))
            .and()
            .exceptionHandling().accessDeniedHandler(customAccessDeniedHandler);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationServiceProvider)
                // needed for change_password
                .eraseCredentials(false);
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

}
