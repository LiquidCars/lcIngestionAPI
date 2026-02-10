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
@Table(name = "op_objt_lcobject_types")
public class JsonObjectTypeEntity {

    @Id
    @Column(name = "objt_co_type")
    private String id;

    @Column(name = "objt_ds_description")
    private String description;

    @Column(name = "objt_ds_preffix")
    private String preffix;

    @Column(name = "objt_nm_sequence_length")
    private Integer sequenceLength;

    @Column(name = "objt_ds_mnemonic_format")
    private String mnemonicFormat;
}
