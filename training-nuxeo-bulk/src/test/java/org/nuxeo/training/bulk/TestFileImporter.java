package org.nuxeo.training.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
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
public class TestFileImporter {
    @Inject
    public ScrollService scrollService;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Inject
    public BulkService bulkService;

    @Test
    public void testFileScroll() throws IOException {
        assertNotNull(scrollService);
        final int size = 13;
        String file = createFile(size);
        ScrollRequest request = FileScrollRequest.builder(file).size(5).build();
        // System.out.println(request);
        assertTrue(scrollService.exists(request));
        try (Scroll scroll = scrollService.scroll(request)) {
            assertNotNull(scroll);
            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("a line 0", "a line 1", "a line 2", "a line 3", "a line 4"), scroll.next());
            assertTrue(scroll.hasNext());
            scroll.next();
            assertTrue(scroll.hasNext());
            assertEquals(Arrays.asList("a line 10", "a line 11", "a line 12"), scroll.next());
            assertFalse(scroll.hasNext());
            try {
                scroll.next();
                fail("Exception expected");
            } catch (NoSuchElementException e) {
                // expected
            }
        }
    }

    protected String createFile(int numberOfLine) throws IOException {
        File tempFile = testFolder.newFile("file.txt");
        try (FileWriter fw = new FileWriter(tempFile, true)) {
            BufferedWriter bw = new BufferedWriter(fw);
            for (int i = 0; i < numberOfLine; i++) {
                bw.write("a line " + i);
                bw.newLine();
            }
            bw.close();
        }
        return tempFile.getAbsolutePath();
    }

    @Test
    public void testImporterFileAction() throws InterruptedException {
        String nxql = "SELECT * from Document";
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
