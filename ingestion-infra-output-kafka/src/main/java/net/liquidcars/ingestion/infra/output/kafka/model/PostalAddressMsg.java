package net.liquidcars.ingestion.infra.output.kafka.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class PostalAddressMsg implements Serializable {
    @Schema(description = "A friendly name for the address")
    private String name;
    @Schema(description = "The GPS location of the address")
    private GPSLocationMsg gpsLocation;
    @Schema(description = "The street number.")
    private String streetNumber;
    @Schema(description = "The street address.")
    private String streetAddress;
    @Schema(description = "The extended address of the address; for example, the apartment number.")
    private String extendedAddress;
    @Schema(description = "The postal code of the address.")
    private String postalCode;
    @Schema(description = "The city of the address.")
    private String city;
    @Schema(description = "The region of the address; for example, the state or province.")
    private String region;
    @Schema(description = "The country of the address.")
    private String country;
    @Schema(description = "The [ISO 3166-1 alpha-2](http://www.iso.org/iso/country_codes.htm) country code of the address.")
    private String countryCode;
    @Schema(description = "The P.O.")
    private String poBox;
    @Schema(description = "The type of the address. Like `home` * `work` * `other`")
    private String type;
}
