package com.comandos.inventory.model;

import com.comandos.core.model.CoreEntity;
import jakarta.persistence.*;

@Entity
@Table(
    name = "erp_armament_parameter",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"parameter_type", "code"}),
        @UniqueConstraint(columnNames = {"parameter_type", "name"})
    }
)
public class ArmamentParameter extends CoreEntity {

    @Column(name = "parameter_type", nullable = false, length = 60, updatable = false)
    public String parameterType;

    @Column(name = "code", nullable = false, length = 80)
    public String code;

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "description", length = 255)
    public String description;

    @Column(name = "active", nullable = false)
    public Boolean active = true;
}
