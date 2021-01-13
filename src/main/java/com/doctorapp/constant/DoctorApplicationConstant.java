package com.doctorapp.constant;

import com.amazonaws.regions.Regions;

public class DoctorApplicationConstant {

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
    public final static String EMAIL_PATTERN =
            "^[_A-Za-z0-9-+]+(.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})$";
}
