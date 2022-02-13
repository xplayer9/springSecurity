package com.top.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.top.model.Top_ModelDTO;
import com.top.repository.Top_Repository;

@Service
public class Top_UserDetailService implements UserDetailsService{
	
	@Autowired
	Top_Repository rep;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
            Top_ModelDTO dto = rep.findByUsername(username).get();
            
            //set roles
            List<GrantedAuthority> list = new ArrayList<GrantedAuthority>();
            list.add(new SimpleGrantedAuthority(dto.getRoles()));
            
            return new User(dto.getUsername(), dto.getPassword(), list);
        } 
		catch (UsernameNotFoundException e) {
			e.printStackTrace();
            throw e;
        }
	}

}
