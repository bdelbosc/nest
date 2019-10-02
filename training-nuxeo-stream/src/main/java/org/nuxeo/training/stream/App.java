package org.nuxeo.training.stream;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.codec.AvroJsonCodec;
import org.nuxeo.lib.stream.codec.AvroMessageCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.codec.SerializableCodec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceListener;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaUtils;

public class App implements RebalanceListener {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(App.class);

    protected static final String APP_TYPE_PROP = "app.type";

    protected static final String CONCURRENCY_PROP = "consumer.concurrency";

    private static final int DEFAULT_CONCURRENCY = 2;

    protected static final String CONSUMER_GROUP_PROP = "consumer.group";

    private static final String DEFAULT_CONSUMER_GROUP = "myGroup";

    protected static final String LOG_TYPE_PROP = "log.type";

    protected static final String LOG_NAME_PROP = "log.name";

    protected static final String LOG_SIZE_PROP = "log.size";

    protected static final String LOG_CODEC_PROP = "log.codec";

    private static final String DEFAULT_LOG_CODEC = "avro";

    private static final String DEFAULT_LOG_NAME = "myLog";

    private static final int DEFAULT_LOG_SIZE = 10;

    protected static final String CQ_PATH_PROP = "cq.path";

    private static final String DEFAULT_CQ_PATH = "/tmp/training";

    protected static final String KAFKA_BOOTSTRAP_PROP = "kafka.bootstrap.servers";

    private static final String DEFAULT_KAFKA_BOOTSTRAP = "localhost:9092";

    protected static final String TIMEOUT_S_PROP = "timeout.seconds";

    private static final long DEFAULT_TIMEOUT_S = 600;

    protected static final String WORK_DURATION_MS_PROP = "work.duration.ms";

    private static final long DEFAULT_WORK_DURATION_MS = 1000;

    public static void main(final String[] args) throws InterruptedException {
        boolean success = new App().run(args);
        if (!success) {
            System.exit(-1);
        }
    }

    public boolean run(String[] args) throws InterruptedException {
        LogManager logManager = useKafka() ? createKafkaLogManager() : createChronicleLogManager();
        String codecName = System.getProperty(LOG_CODEC_PROP, DEFAULT_LOG_CODEC);
        String logName = createLogIfNotExists(logManager, codecName);
        Codec<Record> codec = getCodec(codecName);
        ExecutorService threadPool = isConsumer()
                ? runConsumer(logManager, logName, codec, Long.getLong(WORK_DURATION_MS_PROP, DEFAULT_WORK_DURATION_MS))
                : runProducer(logManager, logName, codec, getInput(args));
        threadPool.shutdown();

        if (!threadPool.awaitTermination(Long.getLong(TIMEOUT_S_PROP, DEFAULT_TIMEOUT_S), TimeUnit.SECONDS)) {
            log.error("Timed out");
            return false;
        }

        log.info("Bye");
        return true;
    }

    @SuppressWarnings("unchecked")
    private Codec<Record> getCodec(String codecStr) {
        switch (codecStr) {
        case "avroJson":
            return new AvroJsonCodec<>(Record.class);
        case "java":
            return new SerializableCodec();
        default:
        case "avro":
            return new AvroMessageCodec<>(Record.class);
        }
    }

    private boolean useKafka() {
        return "kafka".equals(System.getProperty(LOG_TYPE_PROP));
    }

    private boolean isConsumer() {
        return "consumer".equals(System.getProperty(APP_TYPE_PROP));
    }

    private String createLogIfNotExists(LogManager logManager, String codec) {
        String logName = System.getProperty(LOG_NAME_PROP, DEFAULT_LOG_NAME);
        int logSize = Integer.getInteger(LOG_SIZE_PROP, DEFAULT_LOG_SIZE);
        log.debug("Using log: {} with codec: {}", logName, codec);
        if (logManager.createIfNotExists(logName, logSize)) {
            log.info("Log: {} created with {} partitions", logName, logSize);
        } else {
            log.info("Log: {} exists with {} partitions", logName, logManager.size(logName));
        }
        return logName;
    }

    private InputStream getInput(String[] args) {
        if (args != null && args.length > 0) {
            try {
                log.info("Using {} as input", args[0]);
                return new FileInputStream(args[0]);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("Invalid input file", e);
            }
        }
        log.info("Using stdin as input");
        return System.in;
    }

    private ExecutorService runConsumer(LogManager logManager, String logName, Codec<Record> codec, long durationMs) {
        int concurrency = Integer.getInteger(CONCURRENCY_PROP, DEFAULT_CONCURRENCY);
        String group = System.getProperty(CONSUMER_GROUP_PROP, DEFAULT_CONSUMER_GROUP);
        log.info("Running consumer with {} threads", concurrency);
        ExecutorService threadPool = newFixedThreadPool(concurrency, new NamedThreadFactory("consumer"));
        if (useKafka()) {
            for (int i = 0; i < concurrency; i++) {
                LogTailer<Record> tailer = logManager.subscribe(group, Collections.singleton(logName), this, codec);
                threadPool.submit(new Consumer(tailer, durationMs));
            }
            return threadPool;
        }
        // CQ
        int logSize = logManager.size(logName);
        List<List<LogPartition>> assignments = getDefaultAssignments(concurrency, logName, logSize);
        assignments.forEach(parts -> {
            LogTailer<Record> tailer = logManager.createTailer(group, parts, codec);
            threadPool.submit(new Consumer(tailer, durationMs));
        });
        return threadPool;
    }

    private ExecutorService runProducer(LogManager logManager, String logName, Codec<Record> codec,
            InputStream inputStream) {
        LogAppender<Record> appender = logManager.getAppender(logName, codec);
        log.info("Running producer");
        ExecutorService threadPool = newFixedThreadPool(1, new NamedThreadFactory("producer"));
        threadPool.submit(new Producer(appender, inputStream));
        return threadPool;
    }

    private LogManager createChronicleLogManager() {
        Path cqPath = Path.of(System.getProperty(CQ_PATH_PROP, DEFAULT_CQ_PATH));
        log.info("Create a CQ LogManager on: {}", cqPath);
        return new ChronicleLogManager(cqPath);
    }

    private LogManager createKafkaLogManager() {
        log.info("Create a Kafka LogManager {}", this::getBootstrapServers);
        return new KafkaLogManager("training-", getProducerProperties(), getConsumerProperties());
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

    protected List<List<LogPartition>> getDefaultAssignments(int concurrency, String logName, int logSize) {
        Map<String, Integer> streams = new HashMap<>();
        streams.put(logName, logSize);
        return KafkaUtils.roundRobinAssignments(concurrency, streams);
    }

    @Override
    public void onPartitionsRevoked(Collection<LogPartition> partitions) {
        log.debug("Revoked {}", Arrays.toString(partitions.toArray()));
    }

    @Override
    public void onPartitionsAssigned(Collection<LogPartition> partitions) {
        log.debug("Assigned {}", Arrays.toString(partitions.toArray()));
    }
}
