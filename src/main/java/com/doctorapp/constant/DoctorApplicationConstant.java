package com.doctorapp.constant;

import com.amazonaws.regions.Regions;

public class DoctorApplicationConstant {

    /**
     * Constant for User pools
     */
    public final static Regions REGION = Regions.US_WEST_2;
    public final static String DOCTOR_POOL_ID = "us-west-2_OGomw736n";
    public final static String DOCTOR_POOL_CLIENT_ID = "4sjhpah6qk68df70h8rathheqa";
    public final static String EMAIL = "email";
    public final static String USERNAME = "username";
    public final static String TITLE = "custom:title";
    public final static String FIRSTNAME = "given_name";
    public final static String LASTNAME = "family_name";
    public final static String PARTNERNAME = "custom:partner";
    public final static String DOCTOR_FILTER_QUERY = "%s=\"%s\"";


    /**
     * Constant for Frontend check
     */
    public final static String EMAIL_PATTERN =
            "^[_A-Za-z0-9-+]+(.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})$";

    /**
     * Constant for Sessions
     */
    // TODO: change logic for participant status to boolean, so that the text only needs to be reworked on the front end if needed
    public final static String ParticipantStatus_NOTCONNECTTED = "NOT CONNECTED";
    public final static String ParticipantStatus_JOINED = "JOINED";

    /**
     * Constant for Data Access
     */
    public final static String SCHEDULED_TIME = "scheduledTime";
    public final static String FILTER_EXPRESSION = String.format("%s > :startTime and %s  < :endTime",
            SCHEDULED_TIME, SCHEDULED_TIME);
}
