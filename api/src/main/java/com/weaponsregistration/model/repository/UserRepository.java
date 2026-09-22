package com.weaponsregistration.model.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weaponsregistration.model.User;

public interface UserRepository
    extends JpaRepository<User, Long> {

    @Query(
        value = """
            SELECT u.*
            FROM "user" u
            WHERE
                UPPER(COALESCE(u.name, ''))
                    LIKE UPPER(CONCAT('%', :name, '%'))

                AND REGEXP_REPLACE(
                    COALESCE(u.cpf, ''),
                    '[^0-9]',
                    '',
                    'g'
                ) LIKE CONCAT('%', :cpf, '%')

                AND COALESCE(
                    TO_CHAR(
                        u.birth,
                        'DD/MM/YYYY'
                    ),
                    ''
                ) LIKE CONCAT('%', :birth, '%')

                AND UPPER(
                    COALESCE(
                        u.address,
                        ''
                    )
                ) LIKE UPPER(
                    CONCAT(
                        '%',
                        :address,
                        '%'
                    )
                )

                AND UPPER(
                    COALESCE(
                        u.email,
                        ''
                    )
                ) LIKE UPPER(
                    CONCAT(
                        '%',
                        :email,
                        '%'
                    )
                )

                AND REGEXP_REPLACE(
                    COALESCE(
                        u.phone,
                        ''
                    ),
                    '[^0-9]',
                    '',
                    'g'
                ) LIKE CONCAT(
                    '%',
                    :phone,
                    '%'
                )
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM "user" u
            WHERE
                UPPER(COALESCE(u.name, ''))
                    LIKE UPPER(CONCAT('%', :name, '%'))

                AND REGEXP_REPLACE(
                    COALESCE(u.cpf, ''),
                    '[^0-9]',
                    '',
                    'g'
                ) LIKE CONCAT('%', :cpf, '%')

                AND COALESCE(
                    TO_CHAR(
                        u.birth,
                        'DD/MM/YYYY'
                    ),
                    ''
                ) LIKE CONCAT('%', :birth, '%')

                AND UPPER(
                    COALESCE(
                        u.address,
                        ''
                    )
                ) LIKE UPPER(
                    CONCAT(
                        '%',
                        :address,
                        '%'
                    )
                )

                AND UPPER(
                    COALESCE(
                        u.email,
                        ''
                    )
                ) LIKE UPPER(
                    CONCAT(
                        '%',
                        :email,
                        '%'
                    )
                )

                AND REGEXP_REPLACE(
                    COALESCE(
                        u.phone,
                        ''
                    ),
                    '[^0-9]',
                    '',
                    'g'
                ) LIKE CONCAT(
                    '%',
                    :phone,
                    '%'
                )
            """,
        nativeQuery = true
    )
    Page<User> search(
        @Param("name")
        String name,

        @Param("cpf")
        String cpf,

        @Param("birth")
        String birth,

        @Param("address")
        String address,

        @Param("email")
        String email,

        @Param("phone")
        String phone,

        Pageable pageable
    );
}
