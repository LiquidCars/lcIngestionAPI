package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "tl_tl_tiny_locators")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TinyLocatorEntity {

    @Id
    @Column(name = "tl_co_id", length = 10)
    private String id;

    @Column(name = "ofr_co_id")
    private UUID offerId;

    @Column(name = "nin_co_id")
    private UUID inventoryId;

    @Column(name = "agr_co_id")
    private UUID agreementId;

    @Column(name = "ch_co_id")
    private UUID channelId;

    @Column(name = "vsel_co_id")
    private UUID vehicleSellerId;

    @Column(name = "ord_co_id")
    private UUID orderId;
}
