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
@Table(name = "admin_cur_currencies")
public class CurrencyEntity {

    @Id
    @Column(name = "cur_co_id")
    private String id;

    @Column(name = "cur_ds_name")
    private String name;

    @Column(name = "cur_ds_symbol")
    private String symbol;

    @Column(name = "cur_co_iso")
    private Integer iso;

    @Column(name = "cur_nm_decimals")
    private Integer numDecimals;

    @Column(name = "cur_bo_enabled")
    private boolean enabled;
}
