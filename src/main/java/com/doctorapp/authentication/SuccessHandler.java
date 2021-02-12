package com.doctorapp.authentication;

import com.doctorapp.constant.RoleEnum;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

import static com.doctorapp.constant.RoleEnum.*;

@Log4j2
@Component
public class SuccessHandler implements AuthenticationSuccessHandler {

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("Authentication success!");
        String referrer = request.getHeader("Referer");
        log.info("Referer URL: {}", referrer);
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        if (roles.contains(ROLE_CLIENT_ADMIN.name())) {
            response.sendRedirect("/api/partner/token");
        } else if (roles.contains(ROLE_DOCTOR.name())) {
            response.sendRedirect("/view_sessions");
        } else if (roles.contains(ROLE_PATIENT.name())) {
            // TODO: redirect to the page that it came from; currently the problem is referer is login
            response.sendRedirect("/");
        } else if (roles.contains(UNVERIFIED_DOCTOR.name())) {
            response.sendRedirect("/change_password?doctor");
        } else if (roles.contains(UNVERIFIED_PATIENT.name())) {
            response.sendRedirect("/change_password?patient");
        } else {
            throw new IOException("Who is this");
        }
    }
}
