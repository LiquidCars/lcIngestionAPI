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
@Table(name = "op_adt_addresstype")
public class AddressTypeEntity {

    @Id
    @Column(name = "adt_co_id")
    private String id;

    @Column(name = "adt_ds_name")
    private String name;

}
