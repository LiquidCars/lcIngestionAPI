package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "op_pad_participantaddresses")
public class ParticipantAddressEntity {

    @Id
    @Column(name = "par_co_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adt_co_id", nullable = false)
    private AddressTypeEntity addressType;

    @Column(name = "pad_ds_name")
    private String addressName;

    @Column(name = "pad_nm_longitude", columnDefinition = "numeric")
    private double longitude;

    @Column(name = "pad_nm_latitude", columnDefinition = "numeric")
    private double latitude;

    @Column(name = "pad_ds_streetnumber")
    private String streetNumber;

    @Column(name = "pad_ds_streetaddress")
    private String streetAddress;

    @Column(name = "pad_ds_extendedaddress")
    private String extendedAddress;

    @Column(name = "pad_ds_postalcode")
    private String postalCode;

    @Column(name = "pad_ds_city")
    private String city;

    @Column(name = "pad_ds_region")
    private String region;

    @Column(name = "pad_ds_country")
    private String country;

    @Column(name = "pad_ds_countrycode")
    private String countryCode;

    @Column(name = "pad_ds_pobox")
    private String poBox;

}
