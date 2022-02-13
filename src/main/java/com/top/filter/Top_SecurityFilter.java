package com.top.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.top.service.Top_UserDetailService;
import com.top.util.JwtUtil;

import io.jsonwebtoken.ExpiredJwtException;

@Component
public class Top_SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private Top_UserDetailService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;
    
	@Value("${cookieName}")
    private String COOKIE;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        //final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;
        
        //find JWT from cookie
        Cookie[] arry = request.getCookies();
        if(arry!=null) {
	        for(Cookie e:arry) {
	        	if(e.getName().equals(COOKIE)) {
	        		jwt = e.getValue();
	        		break;
	        	}
	        }
        }

        try {
	        if(jwt!=null) 
	            username = jwtUtil.extractUsername(jwt);
        }
        catch(ExpiredJwtException e) {
        	System.out.println("=== JWT expired, delete cookie ");
        	Cookie cookie = new Cookie(COOKIE, "");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }

        if(username != null) {
        	if(SecurityContextHolder.getContext().getAuthentication() == null) {
        		System.out.println("=== in Filter, getAuthentication ");
        		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
	            if(jwtUtil.validateToken(jwt, userDetails)){
	                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
	                        userDetails, null, userDetails.getAuthorities());
	                token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
	                SecurityContextHolder.getContext().setAuthentication(token);
	            }
        	}
        }
        chain.doFilter(request, response);
    }
}
