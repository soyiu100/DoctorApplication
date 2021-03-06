package com.doctorapp.controller;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.cognitoidp.model.UserType;
import com.doctorapp.client.CognitoClient;
import com.doctorapp.dao.PatientDao;
import com.doctorapp.dao.ScheduledSessionDao;
import com.doctorapp.data.Patient;
import com.doctorapp.exception.DependencyException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static com.doctorapp.constant.AWSConfigConstants.*;


/**
 * MVC Controller for {@link RegisterController}
 */
@Controller
@Log4j2
public class SearchPatientController {

    @Autowired
    private PatientDao patientDao;

    @Autowired
    private ScheduledSessionDao scheduledSessionDao;

    @Autowired
    CognitoClient cognitoClient;

    @ResponseBody
    @PostMapping("/search_patient")
    public List<Patient> searchPatient(@RequestParam("firstName") final String firstName,
                                       @RequestParam("lastName") final String lastName,
                                       HttpServletRequest request) {
        // as current cognito only support for 1 attribute as filter
        // search all patients with input last and filter by first name
        log.info("Start to search patient for lastName: {} ", lastName);
        Validate.notBlank(firstName, "The patient's first name cannot be blank.");
        Validate.notBlank(lastName, "The patient's last name cannot be blank.");

        try {
            ListUsersResult userResult =
                    cognitoClient.getPatientIdsByFilter(LASTNAME, lastName.trim());
            log.info("Find {} lastName matched patients", userResult.getUsers().size());
            List<Patient> matchedPatients =
                    filterPatientsByFirstName(userResult.getUsers(), firstName.trim(), lastName.trim());

            log.info("Find {} matched patients", matchedPatients.size());
            matchedPatients.forEach(patient -> {
                log.info("Patient first name is {}, last name is {}, patient ID is {}",
                        patient.getFirstName(), patient.getLastName(), patient.getPatientId());
            });
            return matchedPatients;
        } catch (Exception e) {
            log.error("Failed to search patient" + e.getMessage(), e);
            throw new DependencyException("Failed to search patient", e);
        }
    }

    /**
     * Given a list of patient IDs matching the last name given in the last name parameter, finds patients that could match.
     *
     * @param patients
     * @param firstName
     * @param lastName
     * @return
     */
    private List<Patient> filterPatientsByFirstName(List<UserType> patients,
                                                    String firstName, String lastName) {
        List<Patient> matchedPatients = new ArrayList<>();
        patients.forEach(patientAttr -> {
            List<AttributeType> attributes = patientAttr.getAttributes();

            assert (attributes.size() == 1);

            String patientId = attributes.get(0).getValue();

            Patient patient = patientDao.getPatientById(patientId);

            if (patient.getFirstName().equals(firstName) && patient.getLastName().equals(lastName)) {
                patient.setScheduledSessions(scheduledSessionDao.getScheduledSessionsByPatientId(patientId));
                matchedPatients.add(patient);
            }
        });
        return matchedPatients;
    }

}
