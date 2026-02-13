package net.liquidcars.ingestion.domain.model.batch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class JobDeleteExternalIdsCollector implements Serializable {

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