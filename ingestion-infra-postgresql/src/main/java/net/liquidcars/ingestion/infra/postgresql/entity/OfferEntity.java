package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.liquidcars.ingestion.domain.model.CarOfferResourceDto;
import net.liquidcars.ingestion.domain.model.DateHelperDto;
import net.liquidcars.ingestion.domain.model.MoneyDto;
import net.liquidcars.ingestion.domain.model.ParticipantAddressDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inv_ofr_offer")
public class OfferEntity {

    @Id
    @Column(name = "ofr_co_id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ofr_co_sellertype")
    private CarOfferSellerTypeEnumEntity sellerType;

    @Column(name = "ofr_co_rguownerid")
    private UUID privateOwnerRegisteredUserId;

    @Column(name = "ofr_coid_hash")
    private int hash;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "cari_co_id", nullable = false)
    private VehicleInstanceEntity vehicleInstance;

    @Column(name = "ofr_ownerref")
    private String ownerReference;

    @Column(name = "ofr_dealerref")
    private String dealerReference;

    @Column(name = "ofr_channelref")
    private String channelReference;

    @Column(name = "ofr_nm_price")
    private BigDecimal price;

    @Column(name = "ofr_nm_price_new")
    private BigDecimal priceNew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cur_co_id", nullable = false)
    private CurrencyEntity currency;

    @Column(name = "ofr_nm_financedprice")
    private BigDecimal financedPrice;

    @Column(name = "ofr_ds_financedtext")
    private String financedText;

    @Column(name = "ofr_bo_taxdeductible")
    private boolean taxDeductible;

    @Column(name = "ofr_ds_obs")
    private String obs;

    @Column(name = "ofr_ds_internal_notes")
    private String internalNotes;

    @Column(name = "ofr_bo_guaranteed")
    private boolean guarantee;

    @Column(name = "ofr_nm_guarantee_months")
    private int guaranteeMonths;

    @Column(name = "ofr_ds_guarantee_text")
    private String guaranteeText;

    @Column(name = "ofr_bo_certified")
    private boolean certified;

    @Column(name = "ofr_ds_installation")
    private String installation;

    @Column(name = "ofr_ds_mail")
    private String mail;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "obj_co_jsoncaroffer_id", referencedColumnName = "obj_co_id")
    private JsonOfferEntity jsonCarOffer;

    // añadido
    @Column(name = "ofr_bo_enabled")
    private boolean enabled;

    @Column(name = "ofr_dt_created")
    private OffsetDateTime createdAt;

    @Column(name = "ofr_dt_updated")
    private OffsetDateTime lastUpdated;

}
