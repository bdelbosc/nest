package org.nuxeo.training.importer;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.logging.log4j.Logger;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;

public class App {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(App.class);

    protected static final String LOG_TYPE_PROP = "log.type";

    protected static final String LOG_NAME_PROP = "log.name";

    protected static final String LOG_SIZE_PROP = "log.size";

    private static final String DEFAULT_LOG_NAME = "bjcp";

    private static final int DEFAULT_LOG_SIZE = 4;

    protected static final String CQ_PATH_PROP = "cq.path";

    private static final String DEFAULT_CQ_PATH = "/tmp/training";

    protected static final String KAFKA_BOOTSTRAP_PROP = "kafka.bootstrap.servers";

    private static final String DEFAULT_KAFKA_BOOTSTRAP = "localhost:9092";

    protected static final String TIMEOUT_S_PROP = "timeout.seconds";

    private static final long DEFAULT_TIMEOUT_S = 10;

    public static void main(final String[] args) throws InterruptedException {
        boolean success = new App().run(args);
        if (!success) {
            System.exit(-1);
        }
    }

    public boolean run(String[] args) throws InterruptedException {
        LogManager logManager = useKafka() ? createKafkaLogManager() : createChronicleLogManager();
        String logName = createLogIfNotExists(logManager);
        ExecutorService threadPool = runProducer(logManager, logName, getInputFile(args));
        threadPool.shutdown();
        if (!threadPool.awaitTermination(Long.getLong(TIMEOUT_S_PROP, DEFAULT_TIMEOUT_S), TimeUnit.SECONDS)) {
            log.error("Timed out");
            return false;
        }

        log.info("Bye");
        return true;
    }

    private boolean useKafka() {
        return "kafka".equals(System.getProperty(LOG_TYPE_PROP));
    }

    private String createLogIfNotExists(LogManager logManager) {
        String logName = System.getProperty(LOG_NAME_PROP, DEFAULT_LOG_NAME);
        int logSize = Integer.getInteger(LOG_SIZE_PROP, DEFAULT_LOG_SIZE);
        log.debug("Using log: {}", logName);
        if (logManager.createIfNotExists(logName, logSize)) {
            log.info("Log: {} created with {} partitions", logName, logSize);
        } else {
            log.info("Log: {} exists with {} partitions", logName, logManager.size(logName));
        }
        return logName;
    }

    private String getInputFile(String[] args) {
        if (args != null && args.length > 0) {
            log.info("Using {} as input", args[0]);
            File file = new File(args[0]);
            if (!file.exists()) {
                throw new IllegalArgumentException("File not found: " + file);
            }
            return file.getAbsolutePath();
        }
        throw new IllegalArgumentException("Expecting a JSON file as first argument");
    }

    private ExecutorService runProducer(LogManager logManager, String logName, String filePath) {
        LogAppender<DocumentMessage> appender = logManager.getAppender(logName);
        log.info("Running producer");
        ExecutorService threadPool = newFixedThreadPool(1, new NamedThreadFactory("producer"));
        threadPool.submit(new Producer(appender, filePath));
        return threadPool;
    }

    private LogManager createChronicleLogManager() {
        Path cqPath = Path.of(System.getProperty(CQ_PATH_PROP, DEFAULT_CQ_PATH));
        log.info("Create a CQ LogManager on: {}", cqPath);
        return new ChronicleLogManager(cqPath);
    }

    private LogManager createKafkaLogManager() {
        log.info("Create a Kafka LogManager {}", this::getBootstrapServers);
        return new KafkaLogManager("nuxeo-", getProducerProperties(), getConsumerProperties());
    }

    private Properties getConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 60000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 400);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        return props;
    }

    private String getBootstrapServers() {
        return System.getProperty(KAFKA_BOOTSTRAP_PROP, DEFAULT_KAFKA_BOOTSTRAP);
    }

    private Properties getProducerProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
        return props;
    }
}
