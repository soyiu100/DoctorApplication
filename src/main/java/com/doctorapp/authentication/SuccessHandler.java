package com.doctorapp.authentication;

import com.doctorapp.constant.RoleEnum;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;

import static com.doctorapp.constant.RoleEnum.*;

@Log4j2
@Component
public class SuccessHandler implements AuthenticationSuccessHandler {

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("Authentication success!");
        // gets the referer url
        HttpSession session = request.getSession();
        SavedRequest savedRequest = (SavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");

        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (savedRequest == null || savedRequest.getRedirectUrl().contains("login")
                || roles.contains(UNVERIFIED_DOCTOR.name()) || roles.contains(UNVERIFIED_PATIENT.name())
                || roles.contains(UNVERIFIED_ADMIN.name())
                // force doctor to go to view_sessions
                || (roles.contains(ROLE_DOCTOR.name()) && savedRequest.getRedirectUrl().charAt(savedRequest.getRedirectUrl().length() - 1) == '/')) {
            if (savedRequest != null) {
                log.info("Saved request URL: {}", savedRequest.getRedirectUrl());
            }
            if (roles.contains(ROLE_CLIENT_ADMIN.name()) || roles.contains(ROLE_USER_ADMIN.name())) {
                response.sendRedirect("/partners/form");
            } else if (roles.contains(ROLE_DOCTOR.name())) {
                response.sendRedirect("/view_sessions");
            } else if (roles.contains(ROLE_PATIENT.name())) {
                response.sendRedirect("/");
            } else if (roles.contains(UNVERIFIED_DOCTOR.name())) {
                response.sendRedirect("/change_password?doctor");
            } else if (roles.contains(UNVERIFIED_PATIENT.name())) {
                response.sendRedirect("/change_password?patient");
            } else if (roles.contains(UNVERIFIED_ADMIN.name())) {
                response.sendRedirect("/change_password?admin");
            } else {
                // Should not happen, a bug
                throw new IOException("Who is this");
            }
        } else {
            log.info("Redirecting to {}.", savedRequest.getRedirectUrl());
            response.sendRedirect(savedRequest.getRedirectUrl());
        }

    }
}
