package org.nuxeo.training.bulk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.computation.BulkScrollerComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class MyBulkScrollerComputation extends BulkScrollerComputation {

    private static final Logger log = LogManager.getLogger(MyBulkScrollerComputation.class);

    private final BulkScrollerComputation repoScroller;

    public MyBulkScrollerComputation(String name, int nbOutputStreams, int scrollBatchSize, int scrollKeepAliveSeconds,
            boolean produceImmediate) {
        super(name, nbOutputStreams, scrollBatchSize, scrollKeepAliveSeconds, produceImmediate);
        this.repoScroller = new BulkScrollerComputation(name, nbOutputStreams, scrollBatchSize, scrollKeepAliveSeconds,
                produceImmediate);
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        BulkCommand command = BulkCodecs.getCommandCodec().decode(record.getData());
        if (isRepoCommand(command)) {
            repoScroller.processRecord(context, inputStreamName, record);
            return;
        }
        TransactionHelper.runInTransaction(() -> {
            processRecord(context, record);
        });
    }

    @Override
    protected void processRecord(ComputationContext context, Record record) {
        BulkCommand command = null;

        // TODO: impl elastic scroller
        repoScroller.processRecord(context, "foo", record);
    }

    private boolean isRepoCommand(BulkCommand command) {
        if (command.getQuery().contains("/*+ES:")) {
            log.warn("Using ES Scroller");
            return false;
        }
        log.warn("Using Repo Scroller");
        return true;
    }

}
