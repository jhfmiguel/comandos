package com.comandos.model.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comandos.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
        select u
        from User u
        where upper(coalesce(u.name, '')) like upper(concat('%', concat(:name, '%')))
          and function('regexp_replace', coalesce(u.cpf, ''), '[^0-9]', '') like concat('%', concat(:cpf, '%'))
          and coalesce(function('to_char', u.birth, 'DD/MM/YYYY'), '') like concat('%', concat(:birth, '%'))
          and upper(coalesce(u.address, '')) like upper(concat('%', concat(:address, '%')))
          and upper(coalesce(u.email, '')) like upper(concat('%', concat(:email, '%')))
          and function('regexp_replace', coalesce(u.phone, ''), '[^0-9]', '') like concat('%', concat(:phone, '%'))
        """)
    Page<User> search(
        @Param("name") String name,
        @Param("cpf") String cpf,
        @Param("birth") String birth,
        @Param("address") String address,
        @Param("email") String email,
        @Param("phone") String phone,
        Pageable pageable
    );
}
