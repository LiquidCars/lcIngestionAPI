package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@JobScope // This bean is created fresh for every Job execution
public class JobDeleteExternalIdsCollector {

    private final List<String> deleteIds = Collections.synchronizedList(new ArrayList<>());

    public void addId(String id) {
        deleteIds.add(id);
    }

    public List<String> getDeleteIds() {
        return new ArrayList<>(deleteIds);
    }

    public void clear() {
        deleteIds.clear();
    }
}