package com.doctorapp.controller;

import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.doctorapp.configuration.CognitoClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static com.doctorapp.constant.DoctorApplicationConstant.DOCTOR_POOL_CLIENT_ID;
import static com.doctorapp.constant.DoctorApplicationConstant.DOCTOR_POOL_ID;
import static com.doctorapp.constant.DoctorApplicationConstant.HTTP_SESSIONS_USERNAME;

/**
 * MVC Controller for {@link LoginController}
 */
@Controller
@Log4j2
public class LoginController {

    @Autowired
    CognitoClient cognitoClient;

    @RequestMapping("/login")
    public String loginPage() {
        log.info("seag");
        return "login";
    }

    @RequestMapping("/admin/login")
    public String adminLogin(RedirectAttributes redirect) {
        redirect.addFlashAttribute("admin", true);
        redirect.addAttribute("admin", true);
        return "redirect:login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") final String userName,
                           @RequestParam("password") final String password,
                           HttpServletRequest request,
                           RedirectAttributes redirect ) {
        String newPage = "redirect:login";

        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", userName);
        authParams.put("PASSWORD", password);

        log.info("Start calling cogonito to verify user credential for login," +
                " username {}, password {}", userName, password);
        try {
            AdminInitiateAuthResult authResult = cognitoClient.getAuthResult(DOCTOR_POOL_ID, DOCTOR_POOL_CLIENT_ID, authParams);
            if(authResult.getChallengeName() != null &&
                    authResult.getChallengeName().equals("NEW_PASSWORD_REQUIRED")) {
                log.info("Direct user to change temporary password");
                newPage = "redirect:change_password";
                redirect.addFlashAttribute("username", userName);
                redirect.addFlashAttribute("temp_password", password);
            } else {
                newPage = "redirect:view_sessions";
                //put doctor username into http sessions as access control
                // todo : add more doctor attributes into http sessions if JoinDoctorCall requests required
                request.getSession().setAttribute(HTTP_SESSIONS_USERNAME, userName);
            }
        } catch (Exception e) {
            log.error("Failed to login" + e.getMessage(), e);
            //todo: Error message like : failed to validate your user credential
            redirect.addFlashAttribute("error", true);
        }
        return newPage;
    }

    /**
     * Method to logout customer.
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logoutPage(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/login?logout";
    }


}
