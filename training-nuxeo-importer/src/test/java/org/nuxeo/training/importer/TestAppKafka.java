package org.nuxeo.training.importer;

import static org.junit.Assert.fail;
import static org.nuxeo.training.importer.App.LOG_TYPE_PROP;
import static org.nuxeo.training.importer.App.TIMEOUT_S_PROP;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;

public class TestAppKafka extends org.nuxeo.training.importer.AbstractTestApp {

    @BeforeClass
    public static void assumeKafkaEnabled() {
        if ("true".equals(System.getProperty("kafka"))) {
            if (!KafkaUtils.kafkaDetected()) {
                fail("Kafka profile is enable, no broker found: " + KafkaUtils.getBootstrapServers());
            }
        } else {
            Assume.assumeTrue("No kafka profile", false);
        }
    }

    @Override
    public void setProperties() {
        System.setProperty(TIMEOUT_S_PROP, "5");
        System.setProperty(LOG_TYPE_PROP, "kafka");
        // KAFKA_BOOTSTRAP_PROP is set by maven profile
    }
}
