package com.doctorapp.controller;

import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.doctorapp.client.CognitoClient;
import com.doctorapp.constant.AWSConfigConstants;
import com.doctorapp.dao.ScheduledSessionDao;
import com.doctorapp.data.ScheduledSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.doctorapp.constant.RoleEnum.ROLE_CLIENT_ADMIN;
import static com.doctorapp.constant.RoleEnum.ROLE_DOCTOR;
import static com.doctorapp.constant.RoleEnum.ROLE_USER_ADMIN;

/**
 * MVC Controller for {@link LoginController}
 */
@Controller
@Log4j2
public class LoginController {

    @Autowired
    private ScheduledSessionDao scheduledSessionDao;

    @Autowired
    CognitoClient cognitoClient;

    @RequestMapping("/login")
    public String loginPage(HttpServletRequest request) {
        return "login";
    }

    @RequestMapping("/doctor/login")
    public String doctorLogin(RedirectAttributes redirect) {
        redirect.addFlashAttribute("doctor", true);
        redirect.addAttribute("doctor", true);
        return "redirect:/login";
    }

    @RequestMapping("/admin/login")
    public String adminLogin(RedirectAttributes redirect) {
        redirect.addFlashAttribute("admin", true);
        redirect.addAttribute("admin", true);
        return "redirect:/login";
    }


    /**
     * Method to logout customer.
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logoutPage(HttpServletRequest request, HttpServletResponse response,
                             RedirectAttributes redirect) {
        log.info("Room ID is: {}", request.getParameter("roomId"));
        if (request.getParameter("roomId") != null) {
            ScheduledSession session = scheduledSessionDao.getScheduledSessionByRoomId(request.getParameter("roomId"));
            if (session == null) {
                redirect
                        .addFlashAttribute("errMessage", "No session ID is linked to this session call.");
                return "redirect:error";
            } else {
                session.setDoctorStatus(false);
                scheduledSessionDao.putScheduledSession(session);
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String redirectPage = "redirect:/login?logout";
        if (auth != null) {
            Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());
            if (roles.contains(ROLE_DOCTOR.name())) {
                redirectPage += "&doctor";
            } else if (roles.contains(ROLE_CLIENT_ADMIN.name()) || roles.contains(ROLE_USER_ADMIN.name())) {
                redirectPage += "&admin";
            }
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return redirectPage;
    }


}
