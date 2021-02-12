package com.doctorapp.controller;

import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.doctorapp.client.CognitoClient;
import com.doctorapp.client.PatientDao;
import com.doctorapp.constant.AWSConfigConstants;
import com.doctorapp.data.Doctor;
import com.doctorapp.data.Patient;
import com.doctorapp.exception.DependencyException;
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

import static com.doctorapp.constant.UserTypeConstants.*;

/**
 * MVC Controller for {@link RegisterController}
 */
@Controller
@Log4j2
public class RegisterController {

    @Autowired
    CognitoClient cognitoClient;

    @Autowired
    PatientDao patientDao;

    @RequestMapping("/register")
    public String registerPage() {
        return "register";
    }

    @RequestMapping("/patient/register")
    public String patientRegister() {
        return "patient_register";
    }

    @PostMapping("/create_doctor_form")
    public String register(@RequestParam("username") final String userName,
                           @RequestParam("email") final String emailAddr,
                           @RequestParam("firstname") final String firstname,
                           @RequestParam("lastname") final String lastname,
                           @RequestParam("title") final String title,
                           @RequestParam("partnername") final String partnername,
                           RedirectAttributes redirect) throws Exception {
        String newPage = "redirect:register";
        try {
            validateEmail(emailAddr, DOCTOR);
            validateUsername(userName, DOCTOR);
            log.info("Creating user for userName {}, email address {}, title {}, firstname {}, " +
                    "lastname {}, partner {}", userName, emailAddr, title, firstname, lastname, partnername);
            Doctor doctor = new Doctor(userName, emailAddr, firstname, lastname, title, partnername);
            cognitoClient.createNewUser(doctor);
            redirect.addFlashAttribute("user_name_val", userName);
            //todo: confirmatin message like: register succeed and please check your mailbox for temporary password
            newPage = "redirect:login?doctor";
        } catch (Exception e) {
            log.error("Failed to create user" + e.getMessage(), e);
            throw new DependencyException("Failed to create user", e);
        }
        return newPage;
    }


    @PostMapping("/create_patient_form")
    public String register(@RequestParam("username") final String username,
                           @RequestParam("email") final String emailAddr,
                           @RequestParam("firstName") final String firstName,
                           @RequestParam("lastName") final String lastName,
                           @RequestParam("dob") final String dob,
                           RedirectAttributes redirect) throws Exception {
        String newPage = "redirect:patient/register";
        try {
            validateEmail(emailAddr, PATIENT);
            validateUsername(username, PATIENT);
            log.info("Creating patient for userName {}, email address {}, dateOfBirth {}, firstname {}, " +
                    "lastname {}", username, emailAddr, dob, firstName, lastName);
            Patient patient = new Patient(username, UUID.randomUUID().toString(), emailAddr, firstName, lastName, dob);
            cognitoClient.createNewUser(patient);
            patientDao.putPatient(patient);
            redirect.addFlashAttribute("user_name_val", username);
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
     * @param username
     * @exception InvalidParameterException
     */
    private void validateUsername(String username, String userType) throws Exception{
        try {
            Validate.notBlank(username, "username cannot be blank.");
            String poolID = AWSConfigConstants.DOCTOR_POOL_ID;
            if (userType.equals(PATIENT)) {
                poolID = AWSConfigConstants.PATIENT_POOL_ID;
            }
            ListUsersResult userResult = cognitoClient.getUsersByFilter("username", username, poolID);
            if(userResult != null && userResult.getUsers().size() > 0) {
                throw new Exception("Duplicate user found for username" + username);
            }
            log.info("No duplicate user found for username: {}", username);
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
    private void validateEmail(String emailAddr, String userType) throws Exception {
        try {
            String poolID = AWSConfigConstants.DOCTOR_POOL_ID;
            if (userType.equals(PATIENT)) {
                poolID = AWSConfigConstants.PATIENT_POOL_ID;
            }
            final String EMAIL_PATTERN =
                    "^[_A-Za-z0-9-+]+(.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})$";
            Validate.notBlank(emailAddr, "email cannot be blank.");
            Validate.isTrue(emailAddr.matches(EMAIL_PATTERN), "Invalid email address");
            ListUsersResult userResult = cognitoClient.getUsersByFilter(AWSConfigConstants.EMAIL, emailAddr, poolID);
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
