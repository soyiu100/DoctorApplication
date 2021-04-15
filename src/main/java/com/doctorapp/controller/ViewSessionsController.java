package com.doctorapp.controller;

import com.doctorapp.client.CognitoClient;
import com.doctorapp.dao.PatientDao;
import com.doctorapp.dao.ScheduledSessionDao;
import com.doctorapp.data.Patient;
import com.doctorapp.data.ScheduledSession;
import com.doctorapp.data.TimeRange;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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


    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * Currently view sessions grabs the preferred timezone but doesn't actually translate any times yet
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     *
     * @param model
     * @param request
     * @param redirectAttributes
     * @return
     */
    @RequestMapping(value = "/view_sessions")
    public String viewSessionPage(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        // This is in the case that the doctor clicks home while
        log.info("Room ID is: {}", request.getParameter("roomId"));
        if (request.getParameter("roomId") != null) {
            ScheduledSession session = scheduledSessionDao.getScheduledSessionByRoomId(request.getParameter("roomId"));
            if (session == null) {
                redirectAttributes
                        .addFlashAttribute("errMessage", "No session ID was linked to the session call.");
                return "redirect:error";
            } else {
                session.setDoctorStatus(false);
                scheduledSessionDao.putScheduledSession(session);
            }
        }

        Date date = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String startTime = "";
        String endTime = "";

        if (request.getSession().getAttribute("preferredTimezone") != null) {
            String savedTimezone = (String) request.getSession().getAttribute("preferredTimezone");
            TimeZone.setDefault(TimeZone.getTimeZone(savedTimezone));
        } else {
            // preferred Seattleite timezone ;)
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        }

        if (request.getParameter("startTime") != null && request.getParameter("startTime").length() != 0) {
            log.info("Requested start time is: {}", request.getParameter("startTime"));
            startTime = request.getParameter("startTime");

        } else {
            log.info("Time zone info: {}", TimeZone.getDefault());
            df.setTimeZone(TimeZone.getDefault());

            startTime = df.format(date);
            log.info("Today is: {}", startTime);
        }


        if (request.getParameter("endTime") != null && request.getParameter("endTime").length() != 0) {
            log.info("Requested end time is: {}", request.getParameter("endTime"));
            endTime = request.getParameter("endTime");

        } else {
            endTime = df.format(new Date(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS)));
            log.info("A week from today is: {}", endTime);
        }
        try {
            List<ScheduledSession> sessionsFromDDB = scheduledSessionDao.
                    getScheduledSessionsByTimeRange(new TimeRange(startTime, endTime));

            log.info("get Sessions number: " + sessionsFromDDB.size());
            StringBuilder sessionInfo = new StringBuilder();

            // These will be the list of sessions shown on the website.
            List<ScheduledSession> visibleSessions = new ArrayList<>();

            sessionsFromDDB.forEach(session -> {
                session.setExpired(sessionIsExpired(session.getScheduledTime()));
                visibleSessions.add(parseSessionInfo(session, sessionInfo));
            });

            log.info(sessionInfo);
            model.addAttribute("sessions", visibleSessions);

            model.addAttribute("timeZone", TimeZone.getDefault().getID());

            return "view_sessions";
        } catch (Exception e) {
            redirectAttributes
                    .addFlashAttribute("errMessage", "There was an error parsing session info information: \n" +
                            e.getMessage() + ". Please contact the developers.");
            return "redirect:error";
        }

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
        }

        return "redirect:session_call";
    }

    @RequestMapping(value = "{sess.roomId}/delete")
    public String deleteSession(@PathVariable("sess.roomId") String roomId) {
        log.info("Deleting session: {}", roomId);
        ScheduledSession session = scheduledSessionDao.getScheduledSessionByRoomId(roomId);
        scheduledSessionDao.deleteSession(session);
        return "redirect:/view_sessions";
    }


    @PostMapping("/timezone_change")
    public String changeTimezones(@RequestParam("timeZone") final String timeZone,
                                  HttpServletRequest request) {
        log.info("Request to change time zone: {}", timeZone);
        // US Time Zone recommendations:
        // https://blog.andrewbeacock.com/2006/03/list-of-us-time-zone-ids-for-use-with.html
        switch (timeZone) {
            case "EST":
                request.getSession().setAttribute("preferredTimezone", "America/New_York");
                break;
            case "CST":
                request.getSession().setAttribute("preferredTimezone", "America/Chicago");
                break;
            case "MST":
                request.getSession().setAttribute("preferredTimezone", "America/Denver");
                break;
            case "PST":
                request.getSession().setAttribute("preferredTimezone", "America/Los_Angeles");
                break;
            case "AST":
                request.getSession().setAttribute("preferredTimezone", "America/Anchorage");
                break;
            case "HST":
                request.getSession().setAttribute("preferredTimezone", "America/Adak");
                break;
            default:
                // for now just set the default to UTC
                request.getSession().setAttribute("preferredTimezone", "UTC");
        }
        return "redirect:view_sessions";
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
                            "doctorStatus is %b, " +
                            "patientStatus is %b\n", session.getPatientId(),
                    session.getRoomId(),
                    date, time,
                    session.isDoctorStatus(),
                    session.isPatientStatus());

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
                    .date(date)
                    .time(time)
                    .patient(patient)
                    .expired(session.isExpired())
                    .build();
        } catch (ParseException e) {
            log.error("Failed to parse session time", e);
            return ScheduledSession.builder()
                    .roomId(session.getRoomId())
                    .patientId(session.getPatientId())
                    .doctorStatus(session.isDoctorStatus())
                    .patientStatus(session.isPatientStatus())
                    .expired(session.isExpired())
                    .date(date)
                    .time(time)
                    .build();
        }
    }

    private boolean sessionIsExpired(String sessionDate) {
        try {
            Date scheduledTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(sessionDate);
            // get the time an hour back
            return new Date(System.currentTimeMillis() - 3600 * 1000).after(scheduledTime);
        } catch (ParseException e) {
            log.error("Failed to parse session time", e);
            return false;
        }
    }

}
