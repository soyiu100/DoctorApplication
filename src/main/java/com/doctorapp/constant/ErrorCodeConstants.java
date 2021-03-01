package com.doctorapp.constant;

public class ErrorCodeConstants {

    // When the password is bad. There is no numbers in the password.
    public static final String BAD_PASSWORD_SHORT = "passsword_short";

    // When the password is bad. There is no upper case, which is required of the password.
    public static final String BAD_PASSWORD_UPPER = "passsword_upper";

    // When the password is bad. There is no upper case, which is required of the password.
    public static final String BAD_PASSWORD_LOWER = "passsword_lower";

    // When the password is bad. There is no numbers in the password.
    public static final String BAD_PASSWORD_NUMERIC = "passsword_number";

    // When the password is bad. There is no numbers in the password.
    public static final String BAD_PASSWORD_SPECIAL = "passsword_special";

    // Some unknown error (500)
    public static final String BAD_PASSWORD_UNKNOWN = "err_unknown";

}
