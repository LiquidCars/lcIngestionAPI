package net.liquidcars.ingestion.infra.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostalAddressNoSQLEntity {
    @Field("name")
    private String name;

    @Field("gps_location")
    private GPSLocationNoSQLEntity gpsLocation;

    @Field("street_number")
    private String streetNumber;

    @Field("street_address")
    private String streetAddress;

    @Field("extended_address")
    private String extendedAddress;

    @Field("postal_code")
    private String postalCode;

    @Field("city")
    private String city;

    @Field("region")
    private String region;

    @Field("country")
    private String country;

    @Field("country_code")
    private String countryCode;

    @Field("po_box")
    private String poBox;

    @Field("type")
    private String type;
}
