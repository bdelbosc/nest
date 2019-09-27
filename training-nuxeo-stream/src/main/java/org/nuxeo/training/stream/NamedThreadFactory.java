package org.nuxeo.training.stream;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

public class NamedThreadFactory implements ThreadFactory {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(NamedThreadFactory.class);

    protected final AtomicInteger count = new AtomicInteger(0);

    protected final String prefix;

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, String.format("%s-%02d", prefix, count.getAndIncrement()));
        // note that this does not work for a thread created by an executor submit
        t.setUncaughtExceptionHandler((t1, e) -> log.error("Uncaught exception: " + e.getMessage(), e));
        return t;
    }
}
