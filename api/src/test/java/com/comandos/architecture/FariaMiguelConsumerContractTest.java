package com.comandos.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fariamiguel.audit.api.AuditRecorder;
import com.fariamiguel.core.api.IdGenerator;
import com.fariamiguel.documents.api.DocumentStorage;
import com.fariamiguel.enterprise.catalog.CatalogItem;
import com.fariamiguel.enterprise.contact.PhoneContact;
import com.fariamiguel.enterprise.contracts.Contract;
import com.fariamiguel.enterprise.finance.FinancialEntry;
import com.fariamiguel.enterprise.inventory.StockMovement;
import com.fariamiguel.enterprise.organization.Organization;
import com.fariamiguel.enterprise.party.PartyRoleType;
import com.fariamiguel.enterprise.people.Person;
import com.fariamiguel.enterprise.procurement.PurchaseOrder;
import com.fariamiguel.enterprise.sales.SalesOrder;
import com.fariamiguel.identity.api.IdentityDirectory;
import com.fariamiguel.messaging.api.PlatformEventPublisher;
import com.fariamiguel.notifications.api.NotificationSender;
import com.fariamiguel.security.api.CurrentActorProvider;
import com.fariamiguel.security.api.ResourceAccessPolicy;
import com.fariamiguel.workflow.api.WorkflowEngine;
import org.junit.jupiter.api.Test;

class FariaMiguelConsumerContractTest {

    @Test
    void consumesCanonicalSharedFoundationInsteadOfOwningGenericContracts() {
        assertShared(IdGenerator.class, "com.fariamiguel.core.api");
        assertShared(ResourceAccessPolicy.class, "com.fariamiguel.security.api");
        assertShared(CurrentActorProvider.class, "com.fariamiguel.security.api");
        assertShared(IdentityDirectory.class, "com.fariamiguel.identity.api");
        assertShared(PlatformEventPublisher.class, "com.fariamiguel.messaging.api");
        assertShared(AuditRecorder.class, "com.fariamiguel.audit.api");
        assertShared(DocumentStorage.class, "com.fariamiguel.documents.api");
        assertShared(WorkflowEngine.class, "com.fariamiguel.workflow.api");
        assertShared(NotificationSender.class, "com.fariamiguel.notifications.api");

        assertShared(Person.class, "com.fariamiguel.enterprise.people");
        assertShared(Organization.class, "com.fariamiguel.enterprise.organization");
        assertShared(PhoneContact.class, "com.fariamiguel.enterprise.contact");
        assertShared(PartyRoleType.class, "com.fariamiguel.enterprise.party");
        assertShared(PurchaseOrder.class, "com.fariamiguel.enterprise.procurement");
        assertShared(SalesOrder.class, "com.fariamiguel.enterprise.sales");
        assertShared(FinancialEntry.class, "com.fariamiguel.enterprise.finance");
        assertShared(Contract.class, "com.fariamiguel.enterprise.contracts");
        assertShared(CatalogItem.class, "com.fariamiguel.enterprise.catalog");
        assertShared(StockMovement.class, "com.fariamiguel.enterprise.inventory");
    }

    private static void assertShared(Class<?> type, String expectedPackage) {
        assertThat(type.getPackageName()).isEqualTo(expectedPackage);
        assertThat(type.getName()).startsWith("com.fariamiguel.");
    }
}
