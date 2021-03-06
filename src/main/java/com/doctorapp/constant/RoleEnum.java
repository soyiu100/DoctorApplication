/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License
 * http://aws.amazon.com/asl/
 */
package com.doctorapp.constant;

/**
 * An Enum represents the authentication roles for users and clients.
 *
 * @author Lucun Cai
 */
public enum RoleEnum {
    ROLE_USER_ADMIN, //A role for administrators to manage clients and partners.
    ROLE_CLIENT_ADMIN, //A role for an internal administration OAuth client.
    ROLE_DOCTOR, //A role for an internal administration OAuth client.
    ROLE_PATIENT, //A role for an internal administration OAuth client.
    UNVERIFIED_PATIENT, //A role requires to reset the password
    UNVERIFIED_DOCTOR, //A role requires to reset the password
    UNVERIFIED_ADMIN //A role requires to reset the password
}
