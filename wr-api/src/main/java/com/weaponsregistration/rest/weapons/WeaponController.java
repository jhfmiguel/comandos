package com.weaponsregistration.rest.weapons;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weaponsregistration.model.Weapon;
import com.weaponsregistration.model.repository.WeaponRepository;

@RestController
@RequestMapping("/api/weapons")
@CrossOrigin("*")
public class WeaponController {
	
	private final WeaponRepository repository;

	WeaponController(WeaponRepository repository) {
		this.repository = repository;
	}
	
	@PostMapping
	public WeaponFormRequest save( @RequestBody WeaponFormRequest weapon ) {
		
		Weapon weaponEntity = weapon.toModel();
		repository.save(weaponEntity);
		return WeaponFormRequest.fromModel( weaponEntity );
		
	}
	
	@PutMapping("{id}")
	public ResponseEntity<Void> update( @PathVariable Long id, @RequestBody WeaponFormRequest weapon ) {
		
		Optional<Weapon> existingWeapon = repository.findById(id);
		
		if ( existingWeapon.isEmpty() ) {
			return ResponseEntity.notFound().build();
		}
		
		Weapon entity = weapon.toModel();
		entity.setId( id );
		repository.save( entity );

		return ResponseEntity.ok().build();
	}
	
	@GetMapping
	public List<WeaponFormRequest> getList() {
		
		return repository
				.findAll()
				.stream()
				.map( WeaponFormRequest::fromModel )
				.collect(Collectors.toList());
	}
	
	@GetMapping("{id}")
	public ResponseEntity<WeaponFormRequest> getById (@PathVariable Long id) {
		
		Optional<Weapon> existingWeapon = repository.findById(id);
		
		if( existingWeapon.isEmpty() ) {
			return ResponseEntity.notFound().build();
		}
		
		var weapon = existingWeapon
				.map( WeaponFormRequest::fromModel )
				.get();
		
		return ResponseEntity.ok( weapon );
		
	}
	
	@DeleteMapping("{id}")
	public ResponseEntity<Void> delete( @PathVariable Long id ) {
		
		Optional<Weapon> existingWeapon = repository.findById(id);
		
		if( existingWeapon.isEmpty() ) {
			return ResponseEntity.notFound().build();
		}
		
		repository.delete( existingWeapon.get() );
		
		return ResponseEntity.noContent().build();
		
	}

}
