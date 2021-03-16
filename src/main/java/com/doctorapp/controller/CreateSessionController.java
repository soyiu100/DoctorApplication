package com.doctorapp.controller;

import com.doctorapp.dao.ScheduledSessionDao;
import com.doctorapp.constant.AWSConfigConstants;
import com.doctorapp.data.ScheduledSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.TimeZone;
import java.util.UUID;

/**
 * MVC Controller for {@link CreateSessionController}
 */
@Controller
@Log4j2
public class CreateSessionController {

    @Autowired
    ScheduledSessionDao scheduledSessionDao;

    @RequestMapping("/create_session")
    public String createSessionPage(Model model, HttpServletRequest request) {
        if (request.getSession().getAttribute("preferredTimezone") != null) {
            String savedTimezone = (String) request.getSession().getAttribute("preferredTimezone");
            TimeZone.setDefault(TimeZone.getTimeZone(savedTimezone));
        } else {
            // preferred Seattleite timezone ;)
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        }

        model.addAttribute("timeZone", TimeZone.getDefault().getID());

        return "create_session";
    }

    /**
     * When creating a session, save to DDB as UTC for consistency, and translate UTC time
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * Currently creating a session grabs the preferred timezone but doesn't actually translate any times into DDB
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     *
     * @param patientId
     * @param scheduledTime
     * @param scheduledDate
     * @param request
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/create_session_form")
    public String registerSession(@RequestParam("patientId") final String patientId,
                                  @RequestParam("scheduledDate") final String scheduledDate,
                                  @RequestParam(value = "scheduledTime", required = false, defaultValue = "")  final String scheduledTime,
                                  HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String newPage = "redirect:create_session";
        if (scheduledTime == null || scheduledTime.length() == 0) {
            redirectAttributes.addFlashAttribute("noSessionTime", true);
            return newPage;
        }
        log.info("Creating sessions");
        log.info(scheduledDate);
        log.info(scheduledTime);
        ScheduledSession scheduledSession = new ScheduledSession().toBuilder()
                .patientId(patientId)
                .scheduledTime(scheduledDate + " " + scheduledTime)
                .roomId(UUID.randomUUID().toString())
                .doctorStatus(AWSConfigConstants.ParticipantStatus_NOTCONNECTED)
                .patientStatus(AWSConfigConstants.ParticipantStatus_NOTCONNECTED)
                .build();
        scheduledSessionDao.putScheduledSession(scheduledSession);
        try {
            scheduledSessionDao.putScheduledSession(scheduledSession);
            redirectAttributes.addFlashAttribute("newSessionCreated", true);
            newPage = "redirect:view_sessions";
        } catch (Exception e) {
            log.error("Failed to create session: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("createSessionFailed", true);
        }
        return newPage;
    }
}
