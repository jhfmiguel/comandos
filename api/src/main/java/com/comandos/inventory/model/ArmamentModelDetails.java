package com.comandos.inventory.model;
import com.fariamiguel.app.inventory.model.ItemModel;

import jakarta.persistence.*;
import java.util.Objects;

/** Security-owned attributes of the existing item-model row; it creates no new table. */
@Entity
@Table(name = "erp_item_model")
public class ArmamentModelDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") public Long id;
    @ManyToOne @JoinColumn(name = "armament_type_id") public ArmamentType armamentType;
    @ManyToOne @JoinColumn(name = "armament_classification_id") public ArmamentClassification armamentClassification;

    public static ArmamentModelDetails of(EntityManager em, ItemModel model) {
        ArmamentModelDetails value = model.id == null ? null : em.find(ArmamentModelDetails.class, model.id);
        if (value == null) { value = new ArmamentModelDetails(); value.id = model.id; }
        if (model.extensionValues.containsKey("armamentType"))
            value.armamentType = (ArmamentType) model.extensionValues.get("armamentType");
        if (model.extensionValues.containsKey("armamentClassification"))
            value.armamentClassification = (ArmamentClassification) model.extensionValues.get("armamentClassification");
        return value;
    }
}