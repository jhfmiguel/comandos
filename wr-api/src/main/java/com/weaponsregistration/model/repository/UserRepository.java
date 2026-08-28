package com.weaponsregistration.model.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weaponsregistration.model.User;

public interface UserRepository  extends JpaRepository< User, Long > {
	
	// UserRepository.java
	// UserRepository.java
	@Query( "SELECT c FROM User c WHERE UPPER(c.name) LIKE UPPER(:name) " +
	        "AND REPLACE(REPLACE(c.cpf, '.', ''), '-', '') LIKE CONCAT('%', REPLACE(REPLACE(:cpf, '.', ''), '-', ''), '%')" )
	Page< User > searchByNameOrCPF(
	            @Param("name") String name,
	            @Param("cpf") String cpf, 
	            Pageable pageable
	        );

}
