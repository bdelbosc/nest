package org.nuxeo.training.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.ecm.core.bulk")
@Deploy("org.nuxeo.training.bulk")
@Deploy("org.nuxeo.ecm.platform.tag")
public class TestProcessor {
    @Inject
    public BulkService bulkService;

    @Inject
    public TagService tagService;

    @Inject
    public CoreSession session;

    @Test
    public void testProcessor() throws InterruptedException {
        // create doc
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        List<String> tags = Arrays.asList("foo", "bar");
        doc.setPropertyValue("dc:subjects", (Serializable) tags);
        doc = session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Run a bulk command using repo scroller
        String nxql = "SELECT * from Document";
        assertEquals(1, session.query(nxql).size());
        BulkCommand command = new BulkCommand.Builder("myAction", nxql, session.getPrincipal().getName()).param("fieldName",
                "dc:subjects").repository(session.getRepositoryName()).build();
        String commandId = bulkService.submit(
                command);
        System.out.println(command);
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(10)));

        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(commandId, status.getId());
        assertEquals(COMPLETED, status.getState());
        assertEquals(1, status.getTotal());
        assertEquals(1, status.getProcessed());

        // read invalidations
        session.save();

        Set<String> docTags = tagService.getTags(session, doc.getId());
        assertEquals(new HashSet<>(tags), docTags);
    }
}
