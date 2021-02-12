package com.doctorapp.authentication;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.doctorapp.constant.UserTypeConstants.*;

@Component
@Log4j2
public class FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // TODO: haven't gotten this to work properly yet
//        response.setHeader("Referer", request.getHeader("Referer"));

        log.info("User type found: " + request.getParameter("userType"));
        String userType = request.getParameter("userType");
        String failRedirectURI = "/login?error=true";
        if (userType.equals(DOCTOR)) {
            failRedirectURI += "&doctor=true";
        } else if (userType.equals(ADMIN)) {
            failRedirectURI += "&admin=true";
        }
        response.sendRedirect(failRedirectURI);

    }
}