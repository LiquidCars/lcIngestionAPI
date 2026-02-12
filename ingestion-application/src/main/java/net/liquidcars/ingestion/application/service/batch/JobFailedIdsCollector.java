package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@JobScope // This bean is created fresh for every Job execution
public class JobFailedIdsCollector {
    private final Set<ExternalIdInfoDto> failedIds = Collections.synchronizedSet(new HashSet<>());

    public void addId(ExternalIdInfoDto id) {
        failedIds.add(id);
    }

    public List<ExternalIdInfoDto> getFailedIds() {
        return new ArrayList<>(failedIds);
    }

    public void clear() {
        failedIds.clear();
    }
}