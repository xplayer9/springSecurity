# Java Project -- SpringBoot Security
## Start From Environmental Setup
1. SpringBoot 2.6.3
2. PostgreSQL 14
3. Tomcat 9.0.56 embedded in SpringBoot
4. Eclipse 2021-12
5. Spring Security
6. WebJars Bootstrap and Jquery
7. Thymeleaf
8. Lombok
9. Java 11

# Spring Security Explaination

## 1: Security Configuration
- A class to extends WebSecurityConfigurerAdapter class
- @EnableWebSecurity annotation needed for configuration
- @EnableGlobalMethodSecurity(prePostEnabled = true), in order to use @PreAuthorize annotation
- Override configure(HttpSecurity http), This is to set rules, login page, error page, etc
- Override configure(AuthenticationManagerBuilder auth) This is to register userDetailsService and passwordEncoder
- Override authenticationManagerBean method is necessarry
- Add self defined filter class

```Java
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class Top_SecurityConfiguration extends WebSecurityConfigurerAdapter {
	
	@Autowired
    private UserDetailsService userDetailsService;
	
	@Autowired
	Top_SecurityFilter securityfilter;

	@Override
    protected void configure(HttpSecurity http) throws Exception {
		http
        .authorizeRequests()
        .antMatchers(HttpMethod.GET, "/", "/login", "/logout").permitAll()
        .antMatchers(HttpMethod.POST, "/signin").permitAll()
        .anyRequest().authenticated()
        .and()
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		.and()
        .csrf().disable();
		
        http.formLogin().disable();
		http.logout().disable();
		http
		.addFilterBefore(securityfilter, UsernamePasswordAuthenticationFilter.class);
		
    }
	
	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
	    return super.authenticationManagerBean();
	}
	
	@Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth
		.userDetailsService(userDetailsService)
		.passwordEncoder(new BCryptPasswordEncoder());
    }
```


## Step 2: Implements UserDetailsService
- Use this class to retrive and validate user info/account
- implements loadUserByUsername method, this method return User class with all user's info

```Java
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
```

## Step 3: Self defined filter class
- extends OncePerRequestFilter means to check authetication on every requests
- Override doFilterInternal
- JWT was saved in cookie, so try to retrieve JWT from cookie
- Check and validate JWT
- if all valid, use SecurityContextHolder.getContext().setAuthentication() to authenticate request

```Java
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
```

## Step 4: JWT util class
- To generate/validate JWT token

```Java
@Service
public class JwtUtil {

	@Value("${secretKey}")
    private String SECRET_KEY;
	
	@Value("${keyDuration}")
    private String DURATION;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {

        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + Integer.parseInt(DURATION)))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}

```

