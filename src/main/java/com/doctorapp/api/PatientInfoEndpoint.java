package com.doctorapp.api;

import com.doctorapp.client.PatientDataClient;
import com.doctorapp.data.ScheduledSession;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class PatientInfoEndpoint {

    @Autowired
    PatientDataClient patientDataClient;

    /**
     * Grabs the patient ID associated with the patient linked to the access token.
     *
     * @param accessToken
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/api/patients/accessToken", method = RequestMethod.POST)
    public String getPatientIdWithAccessToken(String accessToken) {
        return patientDataClient.getPatientIdWithAccessToken(accessToken);
    }

    /**
     * TODO: untested but implemented
     * As per documentation,
     * 1) 3P Skill Handler receives the username of the patient and invokes (POST?) to the webapp
     * 2) the webapp grabs the patient ID using the username
     * 3) then searches a DDB table to get the sessions
     *
     * Possible requirements:
     * - Valid CSRF token, as Spring Security is very picky about any POSTs that have a response body
     */
    @ResponseBody
    @RequestMapping(value = "/api/patients/username", method = RequestMethod.POST)
    public List<ScheduledSession> getSessionByUsername(String username) {
        try {
            return patientDataClient.getSessionByUsername(username);
        } catch (Exception e) {
            log.info("Error occurred while getting sessions by patient ID: {}", e.getMessage());
            throw e;
        }
    }
}
