package com.doctorapp.controller;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.cognitoidp.model.UserType;
import com.doctorapp.client.CognitoClient;
import com.doctorapp.client.ScheduledSessionDao;
import com.doctorapp.data.ScheduledSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static com.doctorapp.constant.AWSConfigConstants.FIRSTNAME;
import static com.doctorapp.constant.AWSConfigConstants.LASTNAME;
import static com.doctorapp.constant.AWSConfigConstants.USERNAME;


@Controller
@Log4j2
public class SessionCallController {

    @Autowired
    private ScheduledSessionDao scheduledSessionDao;

    @Autowired
    CognitoClient cognitoClient;

    /**
     * tfw (the function when) the doctor joins the session
     *
     * @param roomId  Session ID pretty much
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
    public String sessionCall(@RequestParam(value = "roomId", required = false, defaultValue = "") final String roomId,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // A room ID is attached to the session! Do things needed for the session
        if (roomId.length() != 0) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            String doctorUsername = auth.getName();
            log.info("Found session for the logged in doctor: {}", doctorUsername);
            if (doctorUsername != null && doctorUsername.length() != 0) {
                ListUsersResult userResult =
                    cognitoClient.getDoctorNameByFilter(USERNAME, doctorUsername.trim());

                assert (userResult.getUsers().size() <= 1);

                if (userResult.getUsers().size() != 0) {
                    UserType doctorUserType = userResult.getUsers().get(0);
                    List<AttributeType> attributes = doctorUserType.getAttributes();

                    assert (attributes.size() == 2);

                    model.addAttribute("roomId", roomId);
                    model.addAttribute("doctorName",
                        attributes.get(0).getValue() + " " + attributes.get(1).getValue());
                    return "session_call";
                } else {
                    // No authenticated user found???
                    log.info(
                        "No authenticated user was found. This is an issue with registering users in Cognito.");
                    return "redirect:login?doctor";
                }
            } else {
                // They aren't logged in?? Spring Security should catch this, but it's here just in case
                log.info(
                    "Spring Security didn't catch that the user was NOT logged in. Check web security configs to make sure that session_call is behind a user role.");
                return "redirect:login?doctor";
            }

        }
        // ...or there is no room ID. This is an error; there should be a room ID linked to the session.
        redirectAttributes
            .addFlashAttribute("errMessage", "No session ID is linked to this session call.");
        return "redirect:error";
    }
}
