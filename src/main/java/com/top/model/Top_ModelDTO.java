package com.top.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name="`users`")
public class Top_ModelDTO {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="id")
    private Long id;
	
	@Column(name="username")
    private String username;
    
    @Column(name="password")
    private String password;
    
    @Column(name="roles")
    private String roles;
}
