/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License
 * http://aws.amazon.com/asl/
 */
package com.doctorapp.configuration;

import com.doctorapp.authentication.AuthenticationServiceProvider;
import com.doctorapp.authentication.RoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/webjars/**", "/resources/**", "/create_user_form");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(authenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .mvcMatchers("/login", "/doctor/login", "/admin/login", "/logout.do", "/css/**", "/js/**", "/actuator/**", "/register", "/create_user_form").permitAll()
            .mvcMatchers("/clients/**", "/partners/**").hasAuthority(RoleEnum.ROLE_USER_ADMIN.name())
            .anyRequest().authenticated()
            .and()
            .formLogin()
            .loginProcessingUrl("/login.do")
            .usernameParameter("username")
            .passwordParameter("password")
            .loginPage("/login")
            .and()
            .logout()
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout.do"));
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationServiceProvider);
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public UsernamePasswordAuthenticationFilter authenticationFilter() {
        UserTypeAuthenticationFilter authFilter = new UserTypeAuthenticationFilter();
        authFilter.setAuthenticationManager(authenticationServiceProvider);
        authFilter.setUsernameParameter("username");
        authFilter.setPasswordParameter("password");
        return authFilter;
    }


}
