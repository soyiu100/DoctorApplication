package com.doctorapp.controller;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cognitoidp.model.AWSCognitoIdentityProviderException;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.InvalidPasswordException;
import com.doctorapp.configuration.CognitoClient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import com.doctorapp.constant.ErrorCodeConstants;

import static com.doctorapp.constant.DoctorApplicationConstant.PATIENT_POOL_CLIENT_ID;
import static com.doctorapp.constant.DoctorApplicationConstant.PATIENT_POOL_ID;

/**
 * MVC Controller for {@link ChangePasswordController}
 */
@Controller
@Log4j2
public class ChangePasswordController {

    private final static String USERNAME = "USERNAME";
    private final static String PASSWORD = "PASSWORD";
    private final static String NEW_PASSWORD = "NEW_PASSWORD";

    @Autowired
    CognitoClient cognitoClient;

    @RequestMapping("/change_password")
    public String changePasswordPage() {
        return "change_password";
    }

    @PostMapping("/change_password_form")
    public String changePassword(@RequestParam("username") final String username,
                           @RequestParam("old_password") final String old_Password,
                           @RequestParam("new_password") final String new_password,
                           @RequestParam("verify_password") final String verify_password,
                           RedirectAttributes redirect ) {
        String newPage = "redirect:change_password";
        log.info("Change password for userName {}", username);
        try {
            validatePassword(new_password, verify_password);
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", username);
            authParams.put("PASSWORD", old_Password);

            log.info("Start calling cogonito to verify user credential for changing password username {}, password {}",
                    username, old_Password);
            // TODO:?????
            // TODO:?????
            // TODO:?????
            // TODO:?????
            // TODO:?????
            AdminInitiateAuthResult authResult = cognitoClient.getAuthResult(PATIENT_POOL_ID, PATIENT_POOL_CLIENT_ID, authParams);
            String authSession = authResult.getSession();

            Map<String, String> challengeResponses = new HashMap<>();
            challengeResponses.put(USERNAME, username);
            challengeResponses.put(PASSWORD, old_Password);
            challengeResponses.put(NEW_PASSWORD, new_password);

            cognitoClient.changeFromTemporaryPassword(challengeResponses, authSession);
            redirect.addFlashAttribute("passwordChanged", true);

            newPage = "redirect:login";
        } catch (InvalidPasswordException ex) {
            log.error("Bad password offered: " + ex.getMessage());
            if (ex.getMessage().contains("uppercase")) {
                redirect.addFlashAttribute("passwordChangeErr", ErrorCodeConstants.BAD_PASSWORD_UPPER);
            } else if (ex.getMessage().contains("numeric")) {
                redirect.addFlashAttribute("passwordChangeErr", ErrorCodeConstants.BAD_PASSWORD_NUMERIC);
            } else if (ex.getMessage().contains("symbol")) {
                redirect.addFlashAttribute("passwordChangeErr", ErrorCodeConstants.BAD_PASSWORD_SPECIAL);
            } else if (ex.getMessage().contains("long enough")) {
                redirect.addFlashAttribute("passwordChangeErr", ErrorCodeConstants.BAD_PASSWORD_SHORT);
            } else {
                redirect.addFlashAttribute("passwordChangeErr", ex.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to change password: " + e.getMessage(), e);

            redirect.addFlashAttribute("passwordChangeErr", e.getMessage());
        }
        return newPage;
    }

    private void validatePassword(String new_Password, String verify_password) throws Exception {
        try {
            Validate.isTrue(StringUtils.equals(new_Password, verify_password),
                    "Password not equal");
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

}
