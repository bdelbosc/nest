package org.nuxeo.training.stream;

import static org.nuxeo.training.stream.App.CQ_PATH_PROP;
import static org.nuxeo.training.stream.App.LOG_TYPE_PROP;
import static org.nuxeo.training.stream.App.TIMEOUT_S_PROP;

import java.io.IOException;
import java.nio.file.Files;

public class TestAppCQ extends AbstractTestApp {
    private static String cqRoot;

    @Override
    public void setProperties() {
        System.setProperty(LOG_TYPE_PROP, "cq");
        System.setProperty(TIMEOUT_S_PROP, "2");
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
