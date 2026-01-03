package com.viecinema.auth.security;

import com.viecinema.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.viecinema.common.constant.SecurityConstant.HEADER_STRING;
import static com.viecinema.common.constant.SecurityConstant.TOKEN_PREFIX;

@Component
@AllArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader(HEADER_STRING);
        final String jwt;
        final String userEmail;
        log.info("Incoming request {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(TOKEN_PREFIX.length());
        try {
            userEmail = jwtService.extractUsername(jwt);
            log.info("Email: {}", userEmail);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
//                  Store request source and info
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                  Set authentication into context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Error in JwtAuthenticationFilter");
            throw e;
        }
        log.info("Direct to controller from : " + request.getRequestURI());
        filterChain.doFilter(request, response);
    }

}
