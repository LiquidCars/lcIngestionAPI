package net.liquidcars.ingestion.infra.mongodb.service.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfferInfraNoSQLMapper {

    OfferNoSQLEntity toEntity(OfferDto offerDto);

    OfferDto toDto(OfferNoSQLEntity offerNoSQLEntity);

    default Instant map(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    default OffsetDateTime map(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
