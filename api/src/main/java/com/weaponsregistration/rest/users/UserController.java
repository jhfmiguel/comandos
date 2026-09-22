package com.weaponsregistration.rest.users;

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

import com.weaponsregistration.model.User;
import com.weaponsregistration.model.repository.UserRepository;

@RestController
@RequestMapping("api/users")
public class UserController {

    private final UserRepository repository;

    UserController(UserRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<UserFormRequest> save(
        @RequestBody UserFormRequest request
    ) {
        User user = request.toModel();

        repository.save(user);

        return ResponseEntity.ok(
            UserFormRequest.fromModel(user)
        );
    }

    @PutMapping("{id}")
    @Transactional
    public ResponseEntity<Void> update(
        @PathVariable Long id,
        @RequestBody UserFormRequest request
    ) {
        Optional<User> existingUser =
            repository.findById(id);

        if (existingUser.isEmpty()) {
            return ResponseEntity
                .notFound()
                .build();
        }

        User user = request.toModel();
        user.setId(id);

        repository.save(user);

        return ResponseEntity
            .noContent()
            .build();
    }

    @GetMapping("{id}")
    public ResponseEntity<UserFormRequest> getById(
        @PathVariable Long id
    ) {
        return repository
            .findById(id)
            .map(UserFormRequest::fromModel)
            .map(ResponseEntity::ok)
            .orElseGet(
                () -> ResponseEntity
                    .notFound()
                    .build()
            );
    }

    @DeleteMapping("{id}")
    @Transactional
    public ResponseEntity<Object> delete(
        @PathVariable Long id
    ) {
        return repository
            .findById(id)
            .map(user -> {
                repository.delete(user);

                return ResponseEntity
                    .noContent()
                    .build();
            })
            .orElseGet(
                () -> ResponseEntity
                    .notFound()
                    .build()
            );
    }

    @GetMapping
    public Page<UserFormRequest> getList(
        @RequestParam(
            required = false,
            defaultValue = ""
        ) String name,

        @RequestParam(
            required = false,
            defaultValue = ""
        ) String cpf,

        @RequestParam(
            required = false,
            defaultValue = ""
        ) String birth,

        @RequestParam(
            required = false,
            defaultValue = ""
        ) String address,

        @RequestParam(
            required = false,
            defaultValue = ""
        ) String email,

        @RequestParam(
            required = false,
            defaultValue = ""
        ) String phone,

        Pageable pageable
    ) {
        return repository
            .search(
                name,
                cpf,
                birth,
                address,
                email,
                phone,
                pageable
            )
            .map(
                UserFormRequest::fromModel
            );
    }
}
