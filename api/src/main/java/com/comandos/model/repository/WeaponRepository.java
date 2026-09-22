package com.comandos.model.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comandos.model.Weapon;

public interface WeaponRepository
    extends JpaRepository<Weapon, Long> {

    @Query(
        value = """
            SELECT w.*
            FROM weapon w
            WHERE
                UPPER(
                    COALESCE(
                        w.sku,
                        ''
                    )
                ) LIKE UPPER(
                    CONCAT(
                        '%',
                        :sku,
                        '%'
                    )
                )

                AND UPPER(
                    COALESCE(
                        w.name,
                        ''
                    )
                ) LIKE UPPER(
                    CONCAT(
                        '%',
                        :name,
                        '%'
                    )
                )

                AND REPLACE(
                    COALESCE(
                        CAST(
                            w.price AS TEXT
                        ),
                        ''
                    ),
                    '.',
                    ','
                ) LIKE CONCAT(
                    '%',
                    REPLACE(
                        :price,
                        '.',
                        ','
                    ),
                    '%'
                )

                AND UPPER(
                    COALESCE(
                        w.description,
                        ''
                    )
                ) LIKE UPPER(
                    CONCAT(
                        '%',
                        :description,
                        '%'
                    )
                )

            ORDER BY w.id ASC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM weapon w
            WHERE
                UPPER(
                    COALESCE(
                        w.sku,
                        ''
                    )
                ) LIKE UPPER(
                    CONCAT(
                        '%',
                        :sku,
                        '%'
                    )
                )

                AND UPPER(
                    COALESCE(
                        w.name,
                        ''
                    )
                ) LIKE UPPER(
                    CONCAT(
                        '%',
                        :name,
                        '%'
                    )
                )

                AND REPLACE(
                    COALESCE(
                        CAST(
                            w.price AS TEXT
                        ),
                        ''
                    ),
                    '.',
                    ','
                ) LIKE CONCAT(
                    '%',
                    REPLACE(
                        :price,
                        '.',
                        ','
                    ),
                    '%'
                )

                AND UPPER(
                    COALESCE(
                        w.description,
                        ''
                    )
                ) LIKE UPPER(
                    CONCAT(
                        '%',
                        :description,
                        '%'
                    )
                )
            """,
        nativeQuery = true
    )
    Page<Weapon> search(
        @Param("sku")
        String sku,

        @Param("name")
        String name,

        @Param("price")
        String price,

        @Param("description")
        String description,

        Pageable pageable
    );
}