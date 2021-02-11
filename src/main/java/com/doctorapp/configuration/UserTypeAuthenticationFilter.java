package com.doctorapp.configuration;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Log4j2
public class UserTypeAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        final String userType = request.getParameter("userType");
        request.getSession().setAttribute("userType", userType);
        log.info("Filter: " + userType);
        // You can do your stuff here
        return super.attemptAuthentication(request, response);
    }
}
