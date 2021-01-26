package com.doctorapp.controller;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.cognitoidp.model.UserType;
import com.doctorapp.configuration.CognitoClient;
import com.doctorapp.exception.DependencyException;
import com.doctorapp.model.Patient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static com.doctorapp.constant.DoctorApplicationConstant.*;


/**
 * MVC Controller for {@link RegisterController}
 */
@Controller
@Log4j2
public class SearchPatientController {

    @Autowired
    CognitoClient cognitoClient;

    @RequestMapping("/search_patient")
    public String registerPage(HttpServletRequest request) {
        log.info("HTTP Session userName is {}",
                request.getSession().getAttribute(HTTP_SESSIONS_USERNAME));

        if(request.getSession().getAttribute(HTTP_SESSIONS_USERNAME) == null) {
            log.info("Session is null, return to login");
            return "redirect:login";
        }
        //todo: map the page to search_patient fragments
        return "search_patient_temp";
    }

    @PostMapping("/search_patient_form")
    public String searchPatient(@RequestParam("patientFirstName") final String patientFirstName,
                        @RequestParam("patientLastName") final String patientLastName,
                        HttpServletRequest request) {
        // as current cognito only support for 1 attribute as filter
        // search all patients with input last and filter by first name
        log.info("Start to search patient for lastName: {} ", patientLastName);
        Validate.notBlank(patientFirstName, "patient FirstName cannot be blank.");
        Validate.notBlank(patientLastName, "patient LastName cannot be blank.");

        try {
            ListUsersResult userResult =
                    cognitoClient.getUsersByFilter(LASTNAME, patientLastName.trim(), PATIENT_POOL_ID);
            log.info("Find {} lastName matched patients", userResult.getUsers().size());
            List<Patient> matchedPatients =
                    filterPatientsByFirstName(userResult.getUsers(), patientFirstName.trim(), patientLastName);

            //todo: fill the patient info into table and return to createSession frontend
            log.info("Find {} matched patients", matchedPatients.size());
            matchedPatients.forEach(patient -> {
                log.info("Patient FirstName is {}, LastName is {}, patientId is {}",
                        patient.getFirstName(), patient.getLastName(), patient.getPatientId());
            });

        } catch (Exception e) {
            //todo: add error for searchPatient
            log.error("Failed to search patient" + e.getMessage(), e);
            throw new DependencyException("Failed to search patient", e);
        }
        return "redirect:create_sessions";
    }

    private List<Patient> filterPatientsByFirstName(List<UserType> patients,
                                                    String firstName, String lastName) {
        List<Patient> matchedPatients = new ArrayList<>();
        patients.forEach(patient -> {
            List<AttributeType> attributes = patient.getAttributes();
            //iterate all attributes to check if firstName matches
            String patientId = "";
            boolean matched = false;
            for(AttributeType attr : attributes) {
                //Because lastName filter in cognito is not case sensitive
                //set firstName matching here not case sensitive as well
                if(StringUtils.equals(FIRSTNAME, attr.getName())
                        && StringUtils.equals(firstName.toLowerCase(), attr.getValue().toLowerCase())) {
                    matched = true;
                } else if(StringUtils.equals(PATIENT_ID, attr.getName())) {
                    patientId = attr.getValue();
                }
            }
            if(matched) {
                matchedPatients.add(new Patient(patientId, firstName, lastName));
            }
        });
        return matchedPatients;
    }

}
