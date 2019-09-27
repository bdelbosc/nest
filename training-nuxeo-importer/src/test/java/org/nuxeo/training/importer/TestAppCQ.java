package org.nuxeo.training.importer;

import static org.nuxeo.training.importer.App.CQ_PATH_PROP;
import static org.nuxeo.training.importer.App.LOG_TYPE_PROP;
import static org.nuxeo.training.importer.App.TIMEOUT_S_PROP;

import java.io.IOException;
import java.nio.file.Files;

public class TestAppCQ extends org.nuxeo.training.importer.AbstractTestApp {
    private static String cqRoot;

    @Override
    public void setProperties() {
        System.setProperty(LOG_TYPE_PROP, "cq");
        System.setProperty(TIMEOUT_S_PROP, "60");
        if (cqRoot == null) {
            try {
                cqRoot = Files.createTempDirectory("trainingStream").toFile().getAbsolutePath();
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to create folder", e);
            }
        }
        System.setProperty(CQ_PATH_PROP, cqRoot);
    }
}
