package com.top.controller;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.top.model.Top_ModelDTO;
import com.top.service.Top_UserDetailService;
import com.top.util.JwtUtil;


@Controller
public class Top_Controller {
	
	@Value("${cookieName}")
    private String COOKIE;
	
	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtUtil jwtTokenUtil;

	@Autowired
	private Top_UserDetailService userDetailsService;

	@GetMapping("/")
	public String getRoot(@ModelAttribute("Top_ModelDTO") Top_ModelDTO dto) {
		return "myLogin";
	}
	
	@GetMapping("/login")
	public String getLogin(@ModelAttribute("Top_ModelDTO") Top_ModelDTO dto) {
		return "myLogin";
	}
	
	@GetMapping("/logout")
	public String getlogout(HttpServletResponse resp, Model model, @ModelAttribute("Top_ModelDTO") Top_ModelDTO dto) {
		model.addAttribute("logout", "true");
		Cookie cookie = new Cookie(COOKIE, "");
        cookie.setMaxAge(0);
        resp.addCookie(cookie);
		return "myLogin";
	}
	
	@PostMapping("/signin")
	public String postSignin(HttpServletResponse resp, Model model, @ModelAttribute("Top_ModelDTO") Top_ModelDTO dto) throws Exception {
		try {
			//authenticationManager check user details
			authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
		}
		catch (InternalAuthenticationServiceException e) {
			model.addAttribute("loginfail", "true");
			System.out.println("=== username not found ");
			return "myLogin";
		}
		catch (BadCredentialsException e) {
			model.addAttribute("loginfail", "true");
			System.out.println("=== Incorrect password ");
			return "myLogin";
		}
		catch (Exception e) {
			model.addAttribute("loginfail", "true");
			System.out.println("=== other errors ");
			return "myLogin";
		}

		UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getUsername());

		String jwt = jwtTokenUtil.generateToken(userDetails);
		Cookie cookie = new Cookie(COOKIE, jwt);
        cookie.setMaxAge(60*60);
		resp.addCookie(cookie);
		return "default";
	}
}
