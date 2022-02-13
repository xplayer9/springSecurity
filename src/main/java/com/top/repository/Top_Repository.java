package com.top.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.top.model.Top_ModelDTO;

@Repository
public interface Top_Repository extends JpaRepository<Top_ModelDTO, Long>{
	
	Optional<Top_ModelDTO> findByUsername(String username);
	
	/*
	@Query(value="select * from stocktable a where a.name = :name", nativeQuery=true)
    List<StockModel> listAll(String name);
	
	@Query(value="select exists(select 1 from tradetable where name = :name)", nativeQuery=true)
    boolean exists(String name);
	
	@Modifying
	@Transactional
	@Query(value="delete from stocktable where stock = :sym", nativeQuery=true)
    void deleteSymbol(String sym);
	
	@Modifying
	@Transactional
	@Query(value="INSERT INTO tradetable VALUES (:name)", nativeQuery=true)
    void insertTable(String name);
    */
}
