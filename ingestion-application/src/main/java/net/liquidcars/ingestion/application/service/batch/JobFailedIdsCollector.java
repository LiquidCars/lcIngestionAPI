package net.liquidcars.ingestion.application.service.batch;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@JobScope // This bean is created fresh for every Job execution
public class JobFailedIdsCollector {
    private final Set<UUID> failedIds = Collections.synchronizedSet(new HashSet<>());

    public void addId(UUID id) {
        failedIds.add(id);
    }

    public List<UUID> getFailedIds() {
        return new ArrayList<>(failedIds);
    }

    public void clear() {
        failedIds.clear();
    }
}