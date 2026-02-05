package net.liquidcars.ingestion.application.service.batch;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@JobScope // This bean is created fresh for every Job execution
public class JobFailedIdsCollector {
    private final Set<String> failedIds = Collections.synchronizedSet(new HashSet<>());

    public void addId(String id) {
        failedIds.add(id);
    }

    public List<String> getFailedIds() {
        return new ArrayList<>(failedIds);
    }
}