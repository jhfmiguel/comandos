package com.weaponsregistration.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weaponsregistration.model.Weapon;

public interface WeaponRepository extends JpaRepository< Weapon, Long > {

}
