package com.doctorapp.controller;

import com.doctorapp.configuration.ScheduledSessionDao;
import com.doctorapp.model.ScheduledSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;


@Controller
@Log4j2
public class SessionCallController {

    @Autowired
    private ScheduledSessionDao scheduledSessionDao;

    /**
     * tfw (the function when) the doctor joins the session
     *
     * @param roomId Session ID pretty much
     * @param request For setting a session open I think
     * @return
     */
    @PostMapping("/disconnect_session")
    public String updateSession(@RequestParam("roomId") final String roomId,
                                HttpServletRequest request,
                                RedirectAttributes redirect) {
        log.info("Request to leave room with room ID: " + roomId);
        // using room ID since patients could have multiple sessions
        ScheduledSession session = scheduledSessionDao.getScheduledSessionByRoomId(roomId);
        if (session == null) {
            redirect.addFlashAttribute("badSession", true);
        } else {
            session.setDoctorStatus(false);
            scheduledSessionDao.putScheduledSession(session);
        }
        return "redirect:view_sessions";
    }


    @RequestMapping("/session_call")
    public String sessionCall(@RequestParam(value="roomId", required=false, defaultValue="") final String roomId,
                              HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (roomId.length() != 0) {
            return "session_call";
        }
        redirectAttributes.addFlashAttribute("errMessage", "No session ID is linked to this session call.");
        return "redirect:error";
    }


}
