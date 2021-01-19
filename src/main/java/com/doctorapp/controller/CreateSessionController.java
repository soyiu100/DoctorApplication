package com.doctorapp.controller;

import com.doctorapp.dao.ScheduledSessionDao;
import com.doctorapp.dto.ScheduledSession;
import com.doctorapp.model.Doctor;
import com.doctorapp.model.ParticipantStatus;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

import static com.doctorapp.constant.DoctorApplicationConstant.ParticipantStatus_NOTCONNECTTED;

/**
 * MVC Controller for {@link CreateSessionController}
 */
@Controller
@Log4j2
public class CreateSessionController {

    @Autowired
    ScheduledSessionDao scheduledSessionDao;

    @RequestMapping("/create_sessions")
    public String createSessionPage() {
        return "create_sessions";
    }

    @PostMapping("/create_session_form")
    public String register(@RequestParam("patientId") final String patientId,
                           @RequestParam("scheduledTime") final String scheduledTime,
                           @RequestParam("durationInMin") final int durationInMin,
                           RedirectAttributes redirect ) throws Exception {
        String newPage = "redirect:create_session_form";
        log.info("Creating sessions");
        //todo: replace patientId info from searchPatient Page
        ScheduledSession scheduledSession = new ScheduledSession().toBuilder()
                .patientId(patientId)
                .scheduledTime(scheduledTime)
                .durationInMin(durationInMin)
                .roomId(UUID.randomUUID().toString())
                .doctorStatus(ParticipantStatus_NOTCONNECTTED)
                .patientStatus(ParticipantStatus_NOTCONNECTTED)
                .build();
        scheduledSessionDao.putScheduledSession(scheduledSession);
        try {
            scheduledSessionDao.putScheduledSession(scheduledSession);
            newPage = "redirect:sessions";
        } catch (Exception e) {
            log.error("Failed to create session: {}", e.getMessage(), e);
            //todo: promote error message on frontend
        }
        return newPage;
    }
}
