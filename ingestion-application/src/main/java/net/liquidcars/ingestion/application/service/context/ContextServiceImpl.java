package net.liquidcars.ingestion.application.service.context;

import net.liquidcars.ingestion.domain.model.security.LCContext;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import org.springframework.stereotype.Service;

@Service
public class ContextServiceImpl implements IContextService {

    // ThreadLocal ensures data is isolated per request/thread
    private static final ThreadLocal<LCContext> userContext = new ThreadLocal<>();

    @Override
    public LCContext getContext() {
        LCContext ctx = userContext.get();
        if (ctx == null) {
            // Return an empty context or a "System" context to avoid NullPointerException
            ctx = createSystemContext();
            userContext.set(ctx);
        }
        return ctx;
    }

    @Override
    public void setContext(LCContext context) {
        userContext.set(context);
    }

    @Override
    public void clear() {
        userContext.remove(); // Vital to avoid memory leaks
    }

    private LCContext createSystemContext() {
        LCContext ctx = new LCContext();
        ctx.setName("SYSTEM_SCHEDULER");
        ctx.setParticipantId("SYSTEM");
        ctx.setRoles(java.util.List.of("LCAdmin"));
        return ctx;
    }
}
