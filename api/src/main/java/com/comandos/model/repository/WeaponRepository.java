package com.comandos.model.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comandos.model.Weapon;

public interface WeaponRepository extends JpaRepository<Weapon, Long> {

    @Query("""
        select w
        from Weapon w
        where upper(coalesce(w.sku, '')) like upper(concat('%', concat(:sku, '%')))
          and upper(coalesce(w.name, '')) like upper(concat('%', concat(:name, '%')))
          and replace(coalesce(cast(w.price as string), ''), '.', ',') like concat('%', concat(replace(:price, '.', ','), '%'))
          and upper(coalesce(w.description, '')) like upper(concat('%', concat(:description, '%')))
        order by w.id asc
        """)
    Page<Weapon> search(
        @Param("sku") String sku,
        @Param("name") String name,
        @Param("price") String price,
        @Param("description") String description,
        Pageable pageable
    );
}
