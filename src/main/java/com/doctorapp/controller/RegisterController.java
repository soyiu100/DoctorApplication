package com.doctorapp.controller;

import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.doctorapp.configuration.CognitoClient;
import com.doctorapp.configuration.PatientDao;
import com.doctorapp.exception.DependencyException;
import com.doctorapp.model.Doctor;
import com.doctorapp.model.Patient;
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

import static com.doctorapp.constant.DoctorApplicationConstant.*;

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

    @RequestMapping("/patient/register")
    public String patientRegister() {
        return "patient_register";
    }

    @RequestMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/create_doctor_form")
    public String register(@RequestParam("username") final String userName,
                           @RequestParam("email") final String emailAddr,
                           @RequestParam("firstname") final String firstname,
                           @RequestParam("lastname") final String lastname,
                           @RequestParam("title") final String title,
                           @RequestParam("partnername") final String partnername,
                           RedirectAttributes redirect ) throws Exception {
        String newPage = "redirect:register";
        validateEmail(emailAddr);
        validateUsername(userName);
        log.info("Creating user for userName {}, email address {}, title {}, firstname {}, " +
                "lastname{}, partner {}", userName, emailAddr, title, firstname, lastname, partnername);
        Doctor doctor = new Doctor(userName, emailAddr, firstname, lastname, title, partnername);
        try {
            cognitoClient.createNewUser(doctor);
            redirect.addFlashAttribute("user_name_val", userName);
            //todo: confirmatin message like: register succeed and please check your mailbox for temporary password
            newPage = "redirect:login";
        } catch (Exception e) {
            log.error("Failed to create user" + e.getMessage(), e);
            throw new DependencyException("Failed to create user", e);
        }
        return newPage;
    }



    @PostMapping("/create_patient_form")
    public String register(@RequestParam("username") final String username,
                           @RequestParam("email") final String emailAddr,
                           @RequestParam("firstname") final String firstName,
                           @RequestParam("lastname") final String lastName,
                           @RequestParam("dateOfBirth") final String dob,
                           RedirectAttributes redirect) throws Exception {
        String newPage = "redirect:register";
        validateEmail(emailAddr);
        validateUsername(username);
        log.info("Creating patient for userName {}, email address {}, dateOfBirth {}, firstname {}, " +
                "lastname {}", username, emailAddr, dob, firstName, lastName);
        Patient patient = new Patient(username, UUID.randomUUID().toString(), emailAddr, firstName, lastName, dob);
        try {
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
     * @param userName
     * @exception InvalidParameterException
     */
    private void validateUsername(String userName) throws DependencyException{
        try {
            Validate.notBlank(userName, "userName cannot be blank.");
            ListUsersResult userResult = cognitoClient.getUsersByFilter(USERNAME, userName, DOCTOR_POOL_ID);
            if(userResult != null && userResult.getUsers().size() > 0) {
                throw new DependencyException("Duplicate user found for userName" + userName);
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
            Validate.notBlank(emailAddr, "email cannot be blank.");
            Validate.isTrue(emailAddr.matches(EMAIL_PATTERN), "Invalid email address");
            ListUsersResult userResult = cognitoClient.getUsersByFilter(EMAIL, emailAddr, DOCTOR_POOL_ID);
            if(userResult != null && userResult.getUsers().size() > 0) {
                throw new DependencyException("Duplicate user found for userName" + emailAddr);
            }
            log.info("No duplicate user found for username: {}", emailAddr);
        } catch (IllegalArgumentException e) {
            //todo: Error message like : emailAddr has exist
            throw new InvalidParameterException(e.getMessage());
        }
    }
}
