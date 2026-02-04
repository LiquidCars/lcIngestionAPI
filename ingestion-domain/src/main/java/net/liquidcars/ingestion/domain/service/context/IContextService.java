package net.liquidcars.ingestion.domain.service.context;

import net.liquidcars.ingestion.domain.model.security.LCContext;

public interface IContextService {
    LCContext getContext();
    void setContext(LCContext context);
    void clear();
}
