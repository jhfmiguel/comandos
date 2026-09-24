package com.comandos.core.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "erp_organizational_unit", uniqueConstraints = {@UniqueConstraint(columnNames = {"organization_id", "code"})})
public class OrganizationalUnit extends CoreEntity {
    
	@ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
   	public Organization organization;
    
	@ManyToOne(optional = true)
    @JoinColumn(name = "parent_unit_id", nullable = true)
    public OrganizationalUnit parentUnit;
    
    @Column(name = "code", nullable = false, length = 255)
    public String code;
    
    @Column(name = "name", nullable = false, length = 255)
    public String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_type_id")
    public OrganizationalUnitType unitType;

    /**
     * Compatibilidade com a coluna histórica "type".
     * Novos cadastros usam unit_type_id; este texto continua sincronizado
     * para preservar bancos existentes durante a transição.
     */
    @Column(name = "type", nullable = false, length = 255)
    public String type;

    @Column(name = "active", nullable = false)
    public Boolean active = true;
    
}
