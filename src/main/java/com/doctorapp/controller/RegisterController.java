package com.doctorapp.controller;

import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.doctorapp.client.CognitoClient;
import com.doctorapp.exception.DependencyException;
import com.doctorapp.model.Doctor;
import lombok.extern.log4j.Log4j2;
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

import static com.doctorapp.constant.DoctorApplicationConstant.*;

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
            return e.getMessage();
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
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(USERNAME, userName);
            ListUsersResult userResult = cognitoClient.getUsersByFilter(queryMap);
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
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EMAIL, emailAddr);
            ListUsersResult userResult = cognitoClient.getUsersByFilter(queryMap);
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
