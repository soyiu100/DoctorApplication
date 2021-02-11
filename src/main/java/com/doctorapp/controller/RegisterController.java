package com.doctorapp.controller;

import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.doctorapp.client.CognitoClient;
import com.doctorapp.constant.DoctorApplicationConstant;
import com.doctorapp.data.Patient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.InvalidParameterException;
import java.util.UUID;


/**
 * MVC Controller for {@link RegisterController}
 */
@Controller
@Log4j2
public class RegisterController {

    @Autowired
    CognitoClient cognitoClient;

    @RequestMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/create_user_form")
    public String register(@RequestParam("username") final String userName,
                           @RequestParam("email") final String emailAddr,
                           @RequestParam("firstname") final String firstname,
                           @RequestParam("lastname") final String lastname,
                           @RequestParam("dateOfBirth") final String dateOfBirth,
                           RedirectAttributes redirect ) throws Exception {
        String newPage = "redirect:register";
        validateEmail(emailAddr);
        validateUsername(userName);
        log.info("Creating patient for userName {}, email address {}, dateOfBirth {}, firstname {}, " +
                "lastname{}", userName, emailAddr, dateOfBirth, firstname, lastname);
        Patient patient = new Patient(userName, UUID.randomUUID().toString(), emailAddr, firstname, lastname, dateOfBirth);
        try {
            cognitoClient.createNewUser(patient);
            redirect.addFlashAttribute("user_name_val", userName);
            //todo: confirmation message like: register succeed and please check your mailbox for temporary password
            newPage = "redirect:login";
        } catch (Exception e) {
            log.error("Failed to create user" + e.getMessage(), e);
            throw new Exception("Failed to create user", e);
        }
        return newPage;
    }

    /**
     * Check whether userName is in correct format
     * Check whether the userName has registered
     *
     * @param userName
     * @exception InvalidParameterException
     */
    private void validateUsername(String userName) throws Exception{
        try {
            Validate.notBlank(userName, "userName cannot be blank.");
            ListUsersResult userResult = cognitoClient.getUsersByFilter("username", userName, DoctorApplicationConstant.PATIENT_POOL_ID);
            if(userResult != null && userResult.getUsers().size() > 0) {
                throw new Exception("Duplicate user found for userName" + userName);
            }
            log.info("No duplicate user found for username: {}", userName);
        } catch (IllegalArgumentException e) {
            //todo: Error message like : username has existed
            throw new InvalidParameterException(e.getMessage());
        }

    }

    /**
     * Check whether email address is in correct format
     * Check whether the email has registered
     *
     * @param emailAddr
     * @exception InvalidParameterException
     */
    private void validateEmail(String emailAddr) throws Exception {
        try {
            final String EMAIL_PATTERN =
                    "^[_A-Za-z0-9-+]+(.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})$";
            Validate.notBlank(emailAddr, "email cannot be blank.");
            Validate.isTrue(emailAddr.matches(EMAIL_PATTERN), "Invalid email address");
            ListUsersResult userResult = cognitoClient.getUsersByFilter(DoctorApplicationConstant.EMAIL, emailAddr, DoctorApplicationConstant.PATIENT_POOL_ID);
            if(userResult != null && userResult.getUsers().size() > 0) {
                throw new Exception("Duplicate user found for email address" + emailAddr);
            }
            log.info("No duplicate user found for email address: {}", emailAddr);
        } catch (IllegalArgumentException e) {
            //todo: Error message like : emailAddr has exist
            throw new InvalidParameterException(e.getMessage());
        }
    }
}
