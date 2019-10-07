package org.nuxeo.training.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
// @Features({ CoreFeature.class, RepositoryElasticSearchFeature.class })
@Features(CoreFeature.class)
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.ecm.core.bulk")
@Deploy("org.nuxeo.training.bulk")
// @Deploy("org.nuxeo.elasticsearch.core")
// @Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
public class TestProcessor {
    @Inject
    public BulkService bulkService;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    public CoreSession session;

    @Test
    public void testProcessor() throws InterruptedException {
        // create doc
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Run a bulk command using repo scroller
        String nxql = "SELECT * from Document";
        assertEquals(1, session.query(nxql).size());
        String commandId = bulkService.submit(
                new BulkCommand.Builder("setProperties", nxql, session.getPrincipal().getName()).repository(
                        session.getRepositoryName()).build());

        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(10)));

        // Assert that it works
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(commandId, status.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(1, status.getTotal());
        assertFalse(status.hasError());

        // Run a bulk command using elastic scroller
        // nxql = "SELECT * from Document WHERE /*+ES: INDEX(ecm:isVersion) */ dc:title = 0";
        nxql = "SELECT * from Document WHERE /*+ES: INDEX(ecm:isVersion) */ ecm:isVersion = 0";
        // return nothing with repository search
        // assertEquals(0, session.query(nxql).size());
        // match one with elastic
        // assertEquals(1, elasticService.query(new NxQueryBuilder(session).nxql(nxql)).totalSize());
        commandId = bulkService.submit(
                new BulkCommand.Builder("setProperties", nxql, session.getPrincipal().getName()).repository(
                        session.getRepositoryName()).build());
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(10)));

        // Assert that it works
        status = bulkService.getStatus(commandId);
        assertEquals(commandId, status.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(1, status.getTotal());
        assertFalse(status.hasError());

    }
}
