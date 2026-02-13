package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "op_obj_lcobject")
public class JsonObjectEntity {

    @Id
    @Column(name = "obj_co_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objt_co_type", nullable = false)
    private JsonObjectTypeEntity jsonObjectType;

    @Column(name = "obj_dt_creation")
    private OffsetDateTime createdAt;

    @Column(name = "obj_ds_mnemonic")
    private String mnemonic;
}
