package com.comandos.model.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comandos.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query(
        value = """
            SELECT u.*
            FROM "user" u
            WHERE UPPER(COALESCE(u.name, '')) LIKE UPPER('%' || :name || '%')
              AND REGEXP_REPLACE(COALESCE(u.cpf, ''), '[^0-9]', '') LIKE '%' || :cpf || '%'
              AND COALESCE(TO_CHAR(u.birth, 'DD/MM/YYYY'), '') LIKE '%' || :birth || '%'
              AND UPPER(COALESCE(u.address, '')) LIKE UPPER('%' || :address || '%')
              AND UPPER(COALESCE(u.email, '')) LIKE UPPER('%' || :email || '%')
              AND REGEXP_REPLACE(COALESCE(u.phone, ''), '[^0-9]', '') LIKE '%' || :phone || '%'
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM "user" u
            WHERE UPPER(COALESCE(u.name, '')) LIKE UPPER('%' || :name || '%')
              AND REGEXP_REPLACE(COALESCE(u.cpf, ''), '[^0-9]', '') LIKE '%' || :cpf || '%'
              AND COALESCE(TO_CHAR(u.birth, 'DD/MM/YYYY'), '') LIKE '%' || :birth || '%'
              AND UPPER(COALESCE(u.address, '')) LIKE UPPER('%' || :address || '%')
              AND UPPER(COALESCE(u.email, '')) LIKE UPPER('%' || :email || '%')
              AND REGEXP_REPLACE(COALESCE(u.phone, ''), '[^0-9]', '') LIKE '%' || :phone || '%'
            """,
        nativeQuery = true
    )
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
