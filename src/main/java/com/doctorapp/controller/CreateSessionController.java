package com.doctorapp.controller;

import com.doctorapp.client.ScheduledSessionDao;
import com.doctorapp.constant.DoctorApplicationConstant;
import com.doctorapp.data.ScheduledSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * MVC Controller for {@link CreateSessionController}
 */
@Controller
@Log4j2
public class CreateSessionController {

    @Autowired
    ScheduledSessionDao scheduledSessionDao;

    @RequestMapping("/create_sessions")
    public String createSessionPage(HttpServletRequest request) {
        log.info("HTTP Session userName is {}",
                request.getSession().getAttribute(DoctorApplicationConstant.HTTP_SESSIONS_USERNAME));

        if (request.getSession().getAttribute(DoctorApplicationConstant.HTTP_SESSIONS_USERNAME) == null) {
            log.info("Session is null, return to login");
            return "redirect:login";
        }
        return "create_sessions";
    }

    @PostMapping("/create_session_form")
    public String register(@RequestParam("patientId") final String patientId,
                           @RequestParam("scheduledTime") final String scheduledTime,
                           @RequestParam("scheduledDate") final String scheduledDate,
                           @RequestParam("durationInMin") final int durationInMin,
                           HttpServletRequest request) {
        String newPage = "redirect:create_sessions";
        log.info("Creating sessions");
        log.info(scheduledDate);
        log.info(scheduledTime);
        //todo: replace patient info from searchPatient Page
        ScheduledSession scheduledSession = new ScheduledSession().toBuilder()
                .patientId(patientId)
                .scheduledTime(scheduledDate + " " + scheduledTime)
                .durationInMin(durationInMin)
                .roomId(UUID.randomUUID().toString())
                .doctorStatus(DoctorApplicationConstant.ParticipantStatus_NOTCONNECTED)
                .patientStatus(DoctorApplicationConstant.ParticipantStatus_NOTCONNECTED)
                .build();
        scheduledSessionDao.putScheduledSession(scheduledSession);
        try {
            scheduledSessionDao.putScheduledSession(scheduledSession);
            //todo: add create session succeed confirmation
            newPage = "redirect:view_sessions";
        } catch (Exception e) {
            log.error("Failed to create session: {}", e.getMessage(), e);
            //todo: promote error message on frontend
        }
        return newPage;
    }
}
