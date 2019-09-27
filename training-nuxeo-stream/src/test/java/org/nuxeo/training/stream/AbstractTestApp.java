package org.nuxeo.training.stream;

import static junit.framework.TestCase.assertTrue;
import static org.nuxeo.training.stream.App.APP_TYPE_PROP;
import static org.nuxeo.training.stream.App.WORK_DURATION_MS_PROP;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public abstract class AbstractTestApp {

    @Before
    public abstract void setProperties();

    @Test
    public void testProducer() throws InterruptedException {
        System.setProperty(APP_TYPE_PROP, "producer");
        File inputFile = new File("src/test/resources/input.txt");
        App app = new App();
        String[] args = new String[1];
        args[0] = inputFile.getAbsolutePath();
        assertTrue(app.run(args));
    }

    @Test
    public void testConsumer() throws InterruptedException {
        // produce some message
        testProducer();
        // consume
        System.setProperty(APP_TYPE_PROP, "consumer");
        System.setProperty(WORK_DURATION_MS_PROP, "1");
        App app = new App();
        app.run(null);
    }

}
