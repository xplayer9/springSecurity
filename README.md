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

## Lombok pre-installation in Eclipse
Lombok is a library that facilitates many tedious tasks and reduces Java source code verbosity<br>
1. Download Lombok jar file
2. Type "java -jar lombok-1.18.4.jar" in concole
3. Installation finish, then restart Eclipse
4. Project should be able to use @Data annotation now


# Spring Security Step by Step

## Step 1: Security Configuration
- "@EnableWebSecurity" annotation to declare security configuration class
- "configure(HttpSecurity http)" used to set rules
- Default login form
```Java
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
	
	@Autowired
  private UserDetailsService userDetailsService;

	@Override
    protected void configure(HttpSecurity http) throws Exception {
		http
        .authorizeRequests()
        .antMatchers(HttpMethod.GET, "/").authenticated()
        .antMatchers(HttpMethod.GET, "/user").hasAnyRole("USER", "VIP")
        .antMatchers(HttpMethod.GET, "/admin").hasAnyRole("ADMIN", "VIP")
        .antMatchers(HttpMethod.GET, "/vip").hasRole("VIP")
        .and()
        .csrf().disable()
        .formLogin().loginPage("/login").permitAll();
    }
	
	@Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth
		.userDetailsService(userDetailsService)
		.passwordEncoder(new BCryptPasswordEncoder());
    }
}
```

## Step 1.1: Inject WebJars Boostrap and Jquery in pom.xml
WebJars are client side dependencies packaged into JAR files, add following dependency in pom.xml
1. WebJars bootstrap 5.1.3
2. WebJars jquery 3.6.0

```xml
<dependency>
	<groupId>org.webjars</groupId>
	<artifactId>bootstrap</artifactId>
	<version>5.1.3</version>
</dependency>
<dependency>
	<groupId>org.webjars</groupId>
	<artifactId>jquery</artifactId>
	<version>3.6.0</version>
</dependency>
```

## Step 2: Add sub-class to the project
 
- Repository: DAO(Data Access Object) layer which connects and accesses to the database
- Service: This layer calls the DAO and perform CRUD operations
- Model: The class mapping to the database table and provides getter and setter functions
- Controller: the class mapping to REST APIs controller for HTTP requests
- DTO: Data to object, used to transfer data between html and controller

### Step 2.1: Model class

```Java
package com.stockApp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name="`stocktable`")
public class StockModel {
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="id")
    private Long id;
	
	@Column(name="name")
    private String name;
    
    @Column(name="stock")
    private String stock;
}
```

### Step 2.2: Repository class

```Java
package com.stockApp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.stockApp.model.StockModel;

@Repository
public interface StockRepository extends JpaRepository<StockModel, Long> {
	
	@Query(value="select * from stocktable a where a.name = :name", nativeQuery=true)
    List<StockModel> listAll(String name);
	
	@Query(value="select exists(select 1 from tradetable where name = :name)", nativeQuery=true)
    boolean exists(String name);
	
	@Modifying
	@Transactional
	@Query(value="delete from stocktable where stock = :sym", nativeQuery=true)
    void deleteSymbol(String sym);
	
	/*
	@Modifying
	@Transactional
	@Query(value="INSERT INTO tradetable VALUES (:name)", nativeQuery=true)
    void insertTable(String name);
    */
}
```

### Step 2.3: Service class

```Java
package com.stockApp.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.stock.dto.StockDTO;
import com.stockApp.model.StockModel;
import com.stockApp.repository.StockRepository;

@Service
public class StockService {
	
	@Autowired
	StockRepository rep;
	
	//Need a free API key from data provider
	private String quoto = "https://financialmodelingprep.com/api/v3/quote/";
	private String apiKey = "?apikey=d1f8636b9df280532258cc61137e6f24";
	
	HttpClient client = HttpClientBuilder.create().build();
	Type aType = new TypeToken<List<StockDTO>>() {}.getType();
	Gson gson = new Gson();
	
	public void addStock(StockDTO dto) {
		StockModel m = new StockModel();
		m.setName("Justin");
		m.setStock(dto.getSymbol());
		rep.save(m);
	}
	
	public void deleteStock(String symbol) {
		rep.deleteSymbol(symbol);
	}

	public List<StockDTO> listAll(String userName) throws Exception {
		
		StringBuilder sb = new StringBuilder(quoto);
		List<StockDTO> ans = new ArrayList<>();
		int len = sb.length();
		
		for(StockModel m:rep.listAll(userName)) {
			sb.append(m.getStock()).append(apiKey);
			System.out.println(sb.toString());
			
			//Use HttpGet to query real-time stock info
			//use Gson to parse Json HttpResponse
	        HttpGet request = new HttpGet(sb.toString());
	        HttpResponse response = client.execute(request);
	        HttpEntity entity = response.getEntity();
	        String content = EntityUtils.toString(entity);
		    List<StockDTO> cur = gson.fromJson(content, aType);
		    ans.add(cur.get(0));
			//
			sb.setLength(len);
		}
	    return ans;
	}
	//Check if stock symbol exist or not
	public boolean exists(String symbol){
		return rep.exists(symbol);
	}
}
```

### Step 2.4: Controller class

```Java
package com.stockApp.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import com.stock.dto.StockDTO;
import com.stockApp.service.StockService;


@Controller
public class StockController {
	
	@Autowired
	StockService service;
	
	private int timeout = 0;
	
	@GetMapping(value="/")
    public String GetDefault() {
        return "redirect:/index";
    }
	
	@GetMapping(value="/delete/{symbol}")
    public String GetDelete(@PathVariable(value = "symbol") String symbol) {
		service.deleteStock(symbol);
        return "redirect:/index";
    }
	
    @PostMapping(value="/add")
    public String postAdd(@ModelAttribute("StockDTO") StockDTO dto) {
    	if(service.exists(dto.getSymbol())) 
    		service.addStock(dto);
  
    	return "redirect:/index"; 
    }
    
    @GetMapping(value="/index")
    public String GetIndex(Model model, @ModelAttribute("StockDTO") StockDTO dto) {
    	String name = "Justin";
    	List<StockDTO> ll = new ArrayList<>();
    	try {
    		ll = service.listAll(name);
    		timeout = 0;
    	}
    	catch(Exception e) {
			//Due to limitation of a free API key
			//Reach query limit, so retry again unless fail 10 times
    		e.printStackTrace();
    		if(++timeout == 10)
    			return "index";
    		else
    			return "redirect:/index";
    	}
    	model.addAttribute("allStocks", ll);
        return "index";
    }
    
}
```
### Step 2.5: DTO class
```Java
package com.stock.dto;

import lombok.Data;

@Data
public class StockDTO {
	private Long id; //pid
	private String userName; //Justin
	private String symbol; //AAPL
	private Double price;
	private Integer volume;
	private String name; //Apple Inc.
	private Double changesPercentage; //6.97776700,
	private Double change; //11.11000100
	private Double previousClose; //159.22000000,
	private int timestamp; //1643582789
	
	private String exchange;
	private String exchangeShortName;
}
```

### Step 2.6: HTML files

Create "index.html" under "src/resources/templates"

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>StockAPP</title>
    <link rel="stylesheet" href="/webjars/bootstrap/5.1.3/css/bootstrap.min.css" />
    <script src="/webjars/jquery/3.6.0/jquery.min.js"></script>
    <script src="/webjars/bootstrap/5.1.3/js/bootstrap.min.js"></script>
</head>
<body> 

<div class="container">
<img th:src="@{/logo.png}" src="../static/logo.png" alt="..." class="img-fluid" />
</div>
<br>
<div class="container">
    <form th:action="@{/add}" method="post" th:object="${StockDTO}">
                <input type="text" class="form-control-sm" required th:field="*{symbol}" placeholder="Enter Stock Symbol"> 
				<button type="submit" class="btn btn-primary">Add Stock</button>
    </form>
</div>

<div class="container">
    <table class="table">
        <thead class="thead-light">
        <tr>
            <th scope="col">Stock</th>
            <th scope="col">Price</th>
            <th scope="col">(+/-)</th>
            <th scope="col">(%)</th>
            <th scope="col">Prev-Closed</th>
            <th scope="col">Volume</th>
            <th scope="col">Delete</th>
        </tr>
        </thead>
        <tbody>
        <tr th:each="StockDTO, iStat : ${allStocks}">
            <th scope="row" th:text="${StockDTO.symbol}">1</th>
            <td th:text="${#numbers.formatCurrency(StockDTO.price)}">2</td>
            <td>
            	<span th:if="${StockDTO.change >= 0}" 
            		  th:text="'+' + ${#numbers.formatCurrency(StockDTO.change)}" class="text-success"> 1</span>
    			<span th:unless="${StockDTO.change >= 0}"
    			      th:text="${#numbers.formatCurrency(StockDTO.change)}" class="text-danger"> 2</span>
            </td>
            <td th:text="${#numbers.formatDecimal(StockDTO.changesPercentage,1,2)} + '%'">4</td>
            <td th:text="${#numbers.formatCurrency(StockDTO.previousClose)}">5</td>
            <td th:text="${StockDTO.volume}">5</td>
            <td><a href="" th:href="@{/delete/{id}(id=${StockDTO.symbol})}" class="btn btn-danger btn-sm">Delete</a></td>
        </tr>
        </tbody>
    </table>
</div>

</body>
</html>
```

## Step 3: Install PostgreSQL Database
 
 [PostgreSQL Download](https://www.postgresql.org/) and installation <br>
 Create user account <br>
 Create database, name: postgres <br>
 Connection configuration: host:localhost, port:1234 <br>
 
 - Pre-generate "stocktable" table: (id: PK, bigint) (name: varchar 255) (stock: varchar 255)
 - Pre-generate "tradetable" table: (stock: varchar 16)
 - Pre-generate hibernate_sequence: "CREATE SEQUENCE public.hibernate_sequence INCREMENT 1 START 1 MINVALUE 1;"
 
## Step 4: Build Application

To connect PostgreSQL, type database details in "application.properties" under "src/main/resources" as following

```Java
# Postgres database, account
spring.datasource.url = jdbc:postgresql://localhost:1234/postgres
spring.datasource.username  = postgres
spring.datasource.password  = 1234
server.port=8080

#spring.datasource.driver-class-name=org.postgresql.Driver
# Keep the connection alive if idle for a long time (needed in production)
spring.datasource.testWhileIdle=true
spring.datasource.validationQuery=SELECT 1
# ===============================
# = JPA / HIBERNATE
# ===============================
# Show or not log for each sql query
spring.jpa.show-sql=true
# Hibernate ddl auto (create, create-drop, update): with "create-drop" the database
# schema will be automatically created afresh for every start of application
# with None, can "CREATE SEQUENCE public.hibernate_sequence INCREMENT 1 START 1 MINVALUE 1"
spring.jpa.hibernate.ddl-auto=create-none

# Naming strategy
#spring.jpa.hibernate.naming.implicit-strategy=org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl
#spring.jpa.hibernate.naming.physical-strategy=org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy

# Allows Hibernate to generate SQL optimized for a particular DBMS
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```
Right click project on Eclipse and select "Run As" then choose "4 Maven build"<br>
In the "main" tab, type "spring-boot:run" in "Goals"<br>
In the "JRE" tab, type "-Dfork=false" in "VM Arguments". So, we can stop Tomcat in Eclipse<br>
Click on "Apply" then "Run"<br>

## Step 5: Test

### Start from http:localhost:8080 or http:localhost:8080/index
- Add or Delete Stock
- Display real-time stock quote
- Page banner image was from "Seeking Alpha" website

<img width="1307" alt="Screenshot11" src="https://user-images.githubusercontent.com/48862763/151896171-ac888296-03dc-4f9f-9e51-2e2d9dc16292.png">
