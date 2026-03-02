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
@Table(name = "op_jobj_json_object")
public class JsonOfferEntity {

    @Id
    @Column(name = "obj_co_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "obj_co_id", nullable = false)
    private JsonObjectEntity jsonObject;

    @Column(name = "jobj_co_class")
    private String jsonOfferClass;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "jobj_ds_json")
    private Map<String, Object> texto;

    @Column(name = "jobj_dt_creation")
    private OffsetDateTime createdAt;
}
