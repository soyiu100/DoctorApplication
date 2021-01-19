package com.doctorapp.controller;

import com.doctorapp.dao.ScheduledSessionDao;
import com.doctorapp.dto.ScheduledSession;
import com.doctorapp.model.TimeRange;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.doctorapp.constant.DoctorApplicationConstant.ParticipantStatus_NOTCONNECTTED;

/**
 * MVC Controller for {@link ViewSessionsController}
 */
@Controller
@Log4j2
public class ViewSessionsController {

    @Autowired
    private ScheduledSessionDao scheduledSessionDao;

    @RequestMapping(value = "/sessions")
    public String viewSessionPage(Model model) {
        String startTime = "2020-01-01";
        String endTime = "2021-01-03";
        List<ScheduledSession> sessions = scheduledSessionDao.
                getScheduledSessionsByTimeRange(new TimeRange(startTime, endTime));

        log.info("get Sessions number: " + sessions.size());
        StringBuilder sessionInfo = new StringBuilder();

        sessions.forEach(session -> {
            //todo : replace the iteration with fill in form
            String sessionTemplate = String.format("Session info is PatientId is %s," +
                    " roomId is %s, scheduledTime is %s, " +
                    "duration is %s, doctorStatus is %s, " +
                    "patientStatus is %s, joinButton is enabled {}\n", session.getPatientId(),
                    session.getRoomId(),
                    session.getScheduledTime(),
                    session.getDurationInMin(),
                    session.getDoctorStatus(),
                    session.getPatientStatus(),
                    ParticipantStatus_NOTCONNECTTED.equals(session.getDoctorStatus()));

            sessionInfo.append(sessionTemplate);
        });

        log.info(sessionInfo);
        model.addAttribute("sessions", sessions);
        return "view_Sessions";

    }

}
