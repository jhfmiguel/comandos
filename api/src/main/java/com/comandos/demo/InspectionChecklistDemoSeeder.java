package com.comandos.demo;

import com.comandos.lifecycle.model.OperationAttachment;
import com.comandos.lifecycle.model.PeriodicInspection;
import com.comandos.lifecycle.model.PeriodicInspectionItem;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(56)
public class InspectionChecklistDemoSeeder implements ApplicationRunner {
    private static final byte[] DEMO_PNG = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
    private final EntityManager em;

    public InspectionChecklistDemoSeeder(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        PeriodicInspection inspection = em.createQuery("select i from PeriodicInspection i order by i.id", PeriodicInspection.class)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
        if (inspection == null) return;
        if (em.createQuery("select count(i) from PeriodicInspectionItem i where i.inspection.id=:id", Long.class)
            .setParameter("id", inspection.id).getSingleResult() > 0) return;

        List<String[]> definitions = List.of(
            new String[]{"SERIAL", "Conferência do número de série", "Conferido com o cadastro patrimonial."},
            new String[]{"INTEGRITY", "Integridade estrutural", "Sem trincas, deformações ou danos aparentes."},
            new String[]{"CLEANING", "Limpeza e conservação", "Condição adequada para emprego operacional."},
            new String[]{"FUNCTION", "Funcionamento", "Mecanismos avaliados sem anormalidade."},
            new String[]{"MAGAZINES", "Carregadores e componentes", "Componentes conferidos e íntegros."},
            new String[]{"ACCESSORIES", "Acessórios vinculados", "Acessórios presentes e compatíveis."}
        );
        int order = 1;
        PeriodicInspectionItem evidenceItem = null;
        for (String[] definition : definitions) {
            PeriodicInspectionItem item = new PeriodicInspectionItem();
            item.inspection = inspection;
            item.itemOrder = order++;
            item.code = definition[0];
            item.label = definition[1];
            item.required = true;
            item.result = "APPROVED";
            item.observation = definition[2];
            em.persist(item);
            if (evidenceItem == null) evidenceItem = item;
        }
        em.flush();

        OperationAttachment photo = new OperationAttachment();
        photo.resource = "periodic-inspections";
        photo.recordId = inspection.id;
        photo.inspectionItem = evidenceItem;
        photo.fileName = "inspecao-integridade-demo.png";
        photo.contentType = "image/png";
        photo.content = DEMO_PNG;
        photo.description = "Evidência fotográfica fictícia vinculada ao item do checklist.";
        photo.uploadedAt = LocalDateTime.now().minusDays(10);
        photo.uploadedById = inspection.responsibleId;
        photo.uploadedByLogin = inspection.responsibleLogin;
        em.persist(photo);
        em.flush();
    }
}
