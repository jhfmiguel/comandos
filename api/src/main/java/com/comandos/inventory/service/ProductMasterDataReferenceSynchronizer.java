package com.comandos.inventory.service;
import com.fariamiguel.app.inventory.service.GenericInventoryReferenceWriter;
import com.fariamiguel.app.core.model.CoreEntity;
import com.fariamiguel.app.core.service.ProductCanonicalScopeResolver;
import com.comandos.custody.model.*;
import com.comandos.consumption.model.AmmunitionConsumption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ProductMasterDataReferenceSynchronizer extends GenericInventoryReferenceWriter {
    public ProductMasterDataReferenceSynchronizer(ProductCanonicalScopeResolver resolver,
            @Value("${comandos.master-data.product-reference-shadow.enabled:false}") boolean enabled) {
        super(resolver, enabled);
    }
    @Override
    protected boolean synchronizeNow(CoreEntity entity) {
        if (entity instanceof Custody custody) {
            synchronizeCustody(custody);
            return true;
        }
        if (entity instanceof CustodyResponsibility responsibility) {
            responsibility.responsiblePersonCanonicalId = require(
                PERSON,
                responsibility.responsiblePersonLegacyId,
                "custody responsibility person"
            );
            return true;
        }
        if (entity instanceof AmmunitionConsumption consumption) {
            consumption.organizationCanonicalId = require(
                ORGANIZATION,
                consumption.organizationLegacyId,
                "ammunition consumption organization"
            );
            consumption.unitCanonicalId = optional(
                UNIT,
                consumption.unitLegacyId,
                "ammunition consumption unit"
            );
            consumption.responsibleCanonicalId = require(
                PERSON,
                consumption.responsibleLegacyId,
                "ammunition consumption responsible"
            );
            consumption.authorizerCanonicalId = require(
                PERSON,
                consumption.authorizerLegacyId,
                "ammunition consumption authorizer"
            );
            return true;
        }
        return super.synchronizeNow(entity);
    }
    private void synchronizeCustody(Custody custody) {
        custody.organizationCanonicalId = require(
            ORGANIZATION,
            custody.organizationLegacyId,
            "custody organization"
        );
        custody.authorizerCanonicalId = require(
            PERSON,
            custody.authorizerLegacyId,
            "custody authorizer"
        );
        custody.unitCanonicalId = optional(
            UNIT,
            custody.unitLegacyId,
            "custody unit"
        );
        custody.recipientCanonicalId = optional(
            PERSON,
            custody.recipientLegacyId,
            "custody recipient"
        );
        custody.recipientUnitCanonicalId = optional(
            UNIT,
            custody.recipientUnitLegacyId,
            "custody recipient unit"
        );
    }}