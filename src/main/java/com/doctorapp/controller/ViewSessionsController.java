package com.doctorapp.controller;

import com.doctorapp.configuration.ScheduledSessionDao;
import com.doctorapp.model.ScheduledSession;
import com.doctorapp.model.TimeRange;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.doctorapp.constant.DoctorApplicationConstant.*;

/**
 * MVC Controller for {@link ViewSessionsController}
 */
@Controller
@Log4j2
public class ViewSessionsController {

    @Autowired
    private ScheduledSessionDao scheduledSessionDao;


    @RequestMapping(value = "/")
    public String indexPage(Model model, HttpServletRequest request) {
        return "redirect:view_sessions";
    }

    @RequestMapping(value = "/view_sessions")
    public String viewSessionPage(Model model, HttpServletRequest request) {
        //if httpSession  is null, return to login
        log.info("HTTP Session userName is {}",
                request.getSession().getAttribute(HTTP_SESSIONS_USERNAME));

        if(request.getSession().getAttribute(HTTP_SESSIONS_USERNAME) == null) {
            return "redirect:login";
        }
        String startTime = "2020-01-01";
        String endTime = "2021-01-03";
        List<ScheduledSession> sessions = scheduledSessionDao.
                getScheduledSessionsByTimeRange(new TimeRange(startTime, endTime));

        log.info("get Sessions number: " + sessions.size());
        StringBuilder sessionInfo = new StringBuilder();

        //todo : replace the iteration with fill in form
        sessions.forEach(session -> {
            parseSessionInfo(session, sessionInfo);
        });

        log.info(sessionInfo);
        model.addAttribute("sessions", sessions);
        return "view_Sessions";
    }

    private void parseSessionInfo(ScheduledSession session, StringBuilder sessionInfo) {
            // original date format would be in format"yyyy-MM-dd HH:mm:ss"
            // extract date and time out from scheduledTime
            try {
                Date scheduledTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(session.getScheduledTime());
                String date = new SimpleDateFormat("yyyy-MM-dd").format(scheduledTime);
                String time = new SimpleDateFormat("HH:mm").format(scheduledTime);

                String sessionTemplate = String.format("Session info is PatientId is %s," +
                                " roomId is %s, scheduled Date is %s, scheduled Time is %s, " +
                                "duration is %s, doctorStatus is %b, " +
                                "patientStatus is %b {}\n", session.getPatientId(),
                        session.getRoomId(),
                        date, time,
                        session.getDurationInMin(),
                        session.isDoctorStatus(),
                        session.isPatientStatus(),
                        session.isDoctorStatus() == ParticipantStatus_NOTCONNECTED);

                sessionInfo.append(sessionTemplate);
            } catch (ParseException e) {
                log.error("Failed to parse session info", e);
                //todo : add an error prompt for session;
            }
    }

}
