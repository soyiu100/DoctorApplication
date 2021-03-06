/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License
 * http://aws.amazon.com/asl/
 */
package com.doctorapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * This the entry point of the application.
 */
@SpringBootApplication
@Configuration
public class DoctorApplication {

    @Autowired
    private SampleDataLoader sampleDataLoader;

    public static void main(String[] args) {
        SpringApplication.run(DoctorApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        sampleDataLoader.loadSampleData();
    }
}
