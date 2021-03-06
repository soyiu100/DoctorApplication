package com.doctorapp.constant;

import com.amazonaws.regions.Regions;

public class AWSConfigConstants {

    /**
     * Constant for User pools
     */
    public final static Regions REGION = Regions.US_WEST_2;
    public final static String DOCTOR_POOL_ID = "us-west-2_6otJZ09gj";
    //            = "us-west-2_OGomw736n";
    public final static String DOCTOR_POOL_CLIENT_ID = "5t3i8q2v58pu1ljr5jlas1kd6n";
    //        = "6kc7sfekf03aij3neap7600k92";

    public final static String PATIENT_POOL_ID = "us-west-2_GmiQVp1cs";
    //        = "us-west-2_fAsZAxMqF";
    public final static String PATIENT_POOL_CLIENT_ID = "v1l0i1he3o812th0rqgnfu5rh";

    public final static String ADMIN_POOL_ID = "us-west-2_dF0DeTVmC";
    public final static String ADMIN_POOL_CLIENT_ID = "2r34dvddn2btt962j30cj62qku";

    public final static String EMAIL = "email";
    public final static String USERNAME = "username";
    public final static String TITLE = "custom:title";
    public final static String FIRSTNAME = "given_name";
    public final static String LASTNAME = "family_name";
    public final static String DOB = "birthdate";
    public final static String PARTNERNAME = "custom:partner";
    public final static String PATIENT_ID = "custom:patientId";
    public final static String USERPOOL_FILTER_QUERY = "%s=\"%s\"";


    /**
     * Constant for Frontend check
     */
    public final static String EMAIL_PATTERN =
            "^[_A-Za-z0-9-+]+(.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})$";

    /**
     * Constant for Meeting Sessions
     */
    public final static boolean ParticipantStatus_NOTCONNECTED = false;
    public final static boolean ParticipantStatus_JOINED = true;

    /**
     * Constant for Data Access
     */
    public final static String SCHEDULED_TIME = "scheduledTime";
    public final static String PATIENTID = "patientId";
    public final static String FILTER_EXPRESSION = String.format("%s > :startTime and %s  < :endTime",
        SCHEDULED_TIME, SCHEDULED_TIME);
    public final static String FILTER_EXPRESSION_WITH_PATIENTID = String.format("%s = :startTime and %s = :patientId",
            SCHEDULED_TIME, PATIENTID);

}
