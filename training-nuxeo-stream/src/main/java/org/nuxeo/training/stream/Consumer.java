/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.training.stream;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.Duration;

import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceException;

public class Consumer implements Runnable {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(Consumer.class);

    private final LogTailer<Record> tailer;

    private final long durationMs;

    public Consumer(LogTailer<Record> tailer, long durationMs) {
        this.tailer = tailer;
        this.durationMs = durationMs;
    }

    @Override
    public void run() {
        try {
            while (true) {
                try {
                    LogRecord<Record> record = tailer.read(Duration.ofSeconds(10));
                    if (record == null) {
                        log.debug("Starving");
                        continue;
                    }
                    process(record);
                    tailer.commit();
                } catch (RebalanceException e) {
                    log.debug("Rebalanced");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted");
        } catch (Exception e) {
            // inside an executor exceptions are part of the result make sure we log them
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            if (tailer != null) {
                tailer.close();
            }
        }
    }

    private void process(LogRecord<Record> record) throws InterruptedException {
        log.debug("Processing {} duration: {}ms", record, durationMs);
        checkPoisonPill(record.message());
        if (durationMs > 0) {
            Thread.sleep(durationMs);
        }
        log.trace("done");
    }

    private void checkPoisonPill(Record record) {
        if (record.getData() == null) {
            return;
        }
        if ("boom".equals(new String(record.getData(), UTF_8))) {
            log.warn("Received poison pill record {}", record);
            throw new RuntimeException("poison pill kill");
        }
    }

}
