package org.nuxeo.training.importer;

import static junit.framework.TestCase.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public abstract class AbstractTestApp {

    @Before
    public abstract void setProperties();

    @Test
    public void testProducer() throws InterruptedException {
        String[] args = new String[2];
        File inputFile = new File("src/test/resources/bjcp-2015.json");
        String parentPath = "/default-domain/workspaces";
        args[0] = inputFile.getAbsolutePath();
        args[1] = parentPath;
        App app = new App();
        assertTrue(app.run(args));
    }

}
