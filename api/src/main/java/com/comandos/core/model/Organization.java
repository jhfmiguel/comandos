package com.comandos.core.model;

import jakarta.persistence.*;

@Entity
@Table(name = "erp_organization")
public class Organization extends CoreEntity {
   
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nature_id")
    public OrganizationNature nature;

    /**
     * Compatibilidade com a coluna historica "nature".
     * Novos cadastros usam nature_id; este texto continua sincronizado
     * para preservar bancos existentes durante a transicao.
     */
    @Column(name = "nature", nullable = false, length = 255)
    public String legacyNature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "economic_activity_id")
    public EconomicActivity economicActivity;
    
	@Column(name = "name", nullable = false, length = 255)
    public String name;
    
	@Column(name = "acronym", nullable = true, length = 255)
    public String acronym;
    
	@Column(name = "tax_id", nullable = true, length = 255)
    public String taxId;
   
	@Column(name = "public_organization", nullable = false)
    public Boolean publicOrganization = false;
    
	@Column(name = "active", nullable = false)
    public Boolean active = true;
	
}
