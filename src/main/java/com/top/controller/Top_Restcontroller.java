package com.top.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Top_Restcontroller {

	@GetMapping("/hello")
	public String gethello() {
		return "Hello";
	}
	
	@PreAuthorize("hasRole('USER')")
	@GetMapping("/user")
	public String getuser() {
		return "Hello User";
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin")
	public String getadmin() {
		return "Hello Admin";
	}
}
