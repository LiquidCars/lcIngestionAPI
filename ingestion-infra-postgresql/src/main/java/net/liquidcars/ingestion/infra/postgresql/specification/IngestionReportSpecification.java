package net.liquidcars.ingestion.infra.postgresql.specification;

import jakarta.persistence.criteria.Predicate;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportFilterDto;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionReportEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class IngestionReportSpecification {

    public static Specification<IngestionReportEntity> filterBy(IngestionReportFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getProcessType() != null)
                predicates.add(cb.equal(root.get("processType"), filter.getProcessType()));

            if (filter.getRequesterParticipantId() != null)
                predicates.add(cb.equal(root.get("requesterParticipantId"), filter.getRequesterParticipantId()));

            if (filter.getInventoryId() != null)
                predicates.add(cb.equal(root.get("inventoryId"), filter.getInventoryId()));

            if (filter.getExternalRequestId() != null && !filter.getExternalRequestId().isBlank())
                predicates.add(cb.equal(root.get("externalRequestId"), filter.getExternalRequestId()));

            if (filter.getStatus() != null)
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));

            if (filter.getDumpType() != null)
                predicates.add(cb.equal(root.get("dumpType"), filter.getDumpType()));

            if (filter.getProcessed() != null)
                predicates.add(cb.equal(root.get("processed"), filter.getProcessed()));

            if (filter.getCreatedFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
            if (filter.getCreatedTo() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo()));

            if (filter.getUpdatedFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), filter.getUpdatedFrom()));
            if (filter.getUpdatedTo() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), filter.getUpdatedTo()));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
