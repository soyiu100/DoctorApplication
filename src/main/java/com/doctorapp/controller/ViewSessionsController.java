package com.doctorapp.controller;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.cognitoidp.model.UserType;
import com.doctorapp.client.CognitoClient;
import com.doctorapp.client.PatientDao;
import com.doctorapp.client.ScheduledSessionDao;
import com.doctorapp.constant.AWSConfigConstants;
import com.doctorapp.data.Patient;
import com.doctorapp.data.ScheduledSession;
import com.doctorapp.data.TimeRange;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.doctorapp.constant.AWSConfigConstants.LASTNAME;
import static com.doctorapp.constant.AWSConfigConstants.PATIENT_POOL_ID;
import static com.doctorapp.constant.AWSConfigConstants.USERNAME;

/**
 * MVC Controller for {@link ViewSessionsController}
 */
@Controller
@Log4j2
public class ViewSessionsController {

    @Autowired
    private ScheduledSessionDao scheduledSessionDao;

    @Autowired
    private PatientDao patientDao;

    @Autowired
    private CognitoClient cognitoClient;

//    @RequestMapping(value = "/")
//    public String indexPage(Model model, HttpServletRequest request) {
//        return "redirect:view_sessions";
//    }

    @RequestMapping(value = "/view_sessions")
    public String viewSessionPage(Model model, HttpServletRequest request) {
        // TODO: wat the fack
        String startTime = "2020-01-01";
        String endTime = "2022-01-03";
        List<ScheduledSession> sessionsFromDDB = scheduledSessionDao.
                getScheduledSessionsByTimeRange(new TimeRange(startTime, endTime));

        log.info("get Sessions number: " + sessionsFromDDB.size());
        StringBuilder sessionInfo = new StringBuilder();

        // These will be the list of sessions shown on the website.
        List<ScheduledSession> visibleSessions = new ArrayList<>();

        // TODO: no idea what the below todo means
        //todo : replace the iteration with fill in form
        sessionsFromDDB.forEach(session -> {
            visibleSessions.add(parseSessionInfo(session, sessionInfo));
        });

        log.info(sessionInfo);
        model.addAttribute("sessions", visibleSessions);
        return "view_sessions";
    }


    /**
     * tfw (the function when) the doctor joins the session
     *
     * @param roomId  Session ID pretty much
     * @param request For setting a session open I think
     * @return
     */
    @PostMapping("/connect_session")
    public String updateSession(@RequestParam("roomId") final String roomId,
                                HttpServletRequest request,
                                RedirectAttributes redirect) {


        log.info("Request to join room with room ID: " + roomId);
        // using room ID since patients could have multiple sessions
        ScheduledSession session = scheduledSessionDao.getScheduledSessionByRoomId(roomId);
        if (session == null) {
            redirect.addFlashAttribute("badSession", true);
        } else {
            session.setDoctorStatus(true);
            scheduledSessionDao.putScheduledSession(session);
            redirect.addAttribute("roomId", roomId);
            // todo: left off here
        }

        return "redirect:session_call";
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
    public List<ScheduledSession> getSessionByPatientId(String username) {
        try {
            ListUsersResult userResult =
                    cognitoClient.getPatientIdsByFilter(USERNAME, username.trim(), PATIENT_POOL_ID);
            List<UserType> patientUserType = userResult.getUsers();
            List<ScheduledSession> matchedSessions = new ArrayList<>();
            assert (patientUserType.size() == 1);

            List<AttributeType> attributes = patientUserType.get(0).getAttributes();

            assert (attributes.size() == 1);

            String patientId = attributes.get(0).getValue();

            return scheduledSessionDao.getScheduledSessionsByPatientId(patientId);
        } catch (Exception e) {
            log.info("Error occurred while getting sessions by patient ID: {}", e.getMessage());
            throw e;
        }
    }

    private ScheduledSession parseSessionInfo(ScheduledSession session, StringBuilder sessionInfo) {
        String date = "";
        String time = "";
        // original date format would be in format"yyyy-MM-dd HH:mm:ss"
        // extract date and time out from scheduledTime
        try {
            Date scheduledTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(session.getScheduledTime());
            date = new SimpleDateFormat("yyyy-MM-dd").format(scheduledTime);
            time = new SimpleDateFormat("hh:mm aaa").format(scheduledTime);

            String sessionTemplate = String.format("Session info is PatientId is %s," +
                            " roomId is %s, scheduled Date is %s, scheduled Time is %s, " +
                            "duration is %s, doctorStatus is %b, " +
                            "patientStatus is %b {}\n", session.getPatientId(),
                    session.getRoomId(),
                    date, time,
                    session.getDurationInMin(),
                    session.isDoctorStatus(),
                    session.isPatientStatus(),
                    session.isDoctorStatus() == AWSConfigConstants.ParticipantStatus_NOTCONNECTED);

            sessionInfo.append(sessionTemplate);

            /* This line doesn't work, because Cognito cannot search users by custom attributes,
             * other than the standard attributes provided by Cognito. */
//            ListUsersResult listUsersResult = cognitoClient.getUsersByFilter("custom:patientId",
//                    session.getPatientId(), PATIENT_POOL_ID);

            // find the patient in the Patients DDB table
            Patient patient = patientDao.getPatientById(session.getPatientId());

            return ScheduledSession.builder()
                    .roomId(session.getRoomId())
                    .patientId(session.getPatientId())
                    .doctorStatus(session.isDoctorStatus())
                    .patientStatus(session.isPatientStatus())
                    .durationInMin(session.getDurationInMin())
                    .date(date)
                    .time(time)
                    .patient(patient)
                    .build();
        } catch (ParseException e) {
            log.error("Failed to parse session time", e);
            //todo : add an error prompt for session;

            // TODO: this is a bad idea but for now
            return null;
        }
    }

}
