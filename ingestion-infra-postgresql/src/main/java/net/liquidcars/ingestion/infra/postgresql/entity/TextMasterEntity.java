package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "str_txm_texts_master")
public class TextMasterEntity {

    @Id
    @Column(name = "txm_co_id")
    private String id;

    @Column(name = "txm_ds_description")
    private String description;

    @Column(name = "txm_bo_enabled")
    private boolean enabled;
}
