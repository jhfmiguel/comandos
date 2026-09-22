package com.comandos.rest.weapons;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comandos.model.Weapon;
import com.comandos.model.repository.WeaponRepository;

@RestController
@RequestMapping("/api/weapons")
public class WeaponController {

    private final WeaponRepository repository;

    public WeaponController(
        WeaponRepository repository
    ) {
        this.repository = repository;
    }

    @PostMapping
    @Transactional
    public WeaponFormRequest save(
        @RequestBody WeaponFormRequest request
    ) {
        Weapon weapon = request.toModel();

        repository.save(weapon);

        return WeaponFormRequest.fromModel(
            weapon
        );
    }

    @PutMapping("{id}")
    @Transactional
    public ResponseEntity<Void> update(
        @PathVariable Long id,
        @RequestBody WeaponFormRequest request
    ) {
        Optional<Weapon> existingWeapon =
            repository.findById(id);

        if (existingWeapon.isEmpty()) {
            return ResponseEntity
                .notFound()
                .build();
        }

        Weapon weapon = request.toModel();

        weapon.setId(id);

        repository.save(weapon);

        return ResponseEntity
            .noContent()
            .build();
    }

    @GetMapping
    public Page<WeaponFormRequest> getList(
        @RequestParam(
            required = false,
            defaultValue = ""
        )
        String sku,

        @RequestParam(
            required = false,
            defaultValue = ""
        )
        String name,

        @RequestParam(
            required = false,
            defaultValue = ""
        )
        String price,

        @RequestParam(
            required = false,
            defaultValue = ""
        )
        String description,

        Pageable pageable
    ) {
        return repository
            .search(
                sku,
                name,
                price,
                description,
                pageable
            )
            .map(
                WeaponFormRequest::fromModel
            );
    }

    @GetMapping("{id}")
    public ResponseEntity<WeaponFormRequest> getById(
        @PathVariable Long id
    ) {
        return repository
            .findById(id)
            .map(
                WeaponFormRequest::fromModel
            )
            .map(
                ResponseEntity::ok
            )
            .orElseGet(
                () -> ResponseEntity
                    .notFound()
                    .build()
            );
    }

    @DeleteMapping("{id}")
    @Transactional
    public ResponseEntity<Void> delete(
        @PathVariable Long id
    ) {
        Optional<Weapon> existingWeapon =
            repository.findById(id);

        if (existingWeapon.isEmpty()) {
            return ResponseEntity
                .notFound()
                .build();
        }

        repository.delete(
            existingWeapon.get()
        );

        return ResponseEntity
            .noContent()
            .build();
    }
}
