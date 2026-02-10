package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inv_cr_carresources")
public class CarOfferResourceEntity {

    @Id
    @Column(name = "cr_co_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ofr_co_id")
    private OfferEntity offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rt_co_id")
    private ResourceTypeEntity resourceType;

    @Column(name = "cr_ds_resource")
    private byte[] resource;
}
