package com.weaponsregistration.rest.users;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // IMPORTED
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.weaponsregistration.model.User;
import com.weaponsregistration.model.repository.UserRepository;

@RestController
@RequestMapping( "api/users" )
@CrossOrigin("*")
public class UserController {
	
	@Autowired
	private UserRepository repository;
	
	@PostMapping
	@Transactional // ENABLES: Forces database transaction to COMMIT on success
	public ResponseEntity save( @RequestBody UserFormRequest request ) {
		
		User user = request.toModel();
		repository.save( user );
		
		return ResponseEntity.ok( UserFormRequest.fromModel( user ) );
		
	}
	
	@PutMapping( "{id}" )
	@Transactional // ENABLES: Ensures updates are successfully committed
	public ResponseEntity<Void> update( @PathVariable Long id, @RequestBody UserFormRequest request ) {
		
		Optional< User > existingUser = repository.findById( id );
		if( existingUser.isEmpty() ) { return ResponseEntity.notFound().build(); }
		
		User user = request.toModel();
		user.setId( id );
		repository.save( user );
		
		return ResponseEntity.noContent().build();
		
	}
	
	@GetMapping( "{id}" )
	public ResponseEntity< UserFormRequest > getById( @PathVariable Long id ) {
		
		return repository.findById( id )
				.map( UserFormRequest::fromModel )
				.map( userFR -> ResponseEntity.ok( userFR ) )
				.orElseGet( () -> ResponseEntity.notFound().build() );
	
	}
	
	@DeleteMapping( "{id}" )
	@Transactional // ENABLES: Forces row deletion block to commit changes
	public ResponseEntity< Object > delete( @PathVariable Long id ) {
		
		return repository
				.findById( id )
				.map( user -> {
					repository.delete( user );
					return ResponseEntity.noContent().build();
				} )
				.orElseGet( () -> ResponseEntity.notFound().build() );
				
	}
	
	@GetMapping
	public Page< UserFormRequest > getList(
			@RequestParam( required = false, defaultValue = "" ) String name,
			@RequestParam( required = false, defaultValue = ""  ) String cpf,
			Pageable pageable
			
			) {
		return repository
				.searchByNameOrCPF(
						"%" + name + "%",  
						"%" + cpf + "%", 
						pageable
					)
				.map(  UserFormRequest::fromModel );		
				
	}
}
