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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogOffset;

public class Producer implements Runnable {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(Producer.class);

    private static final String EOF = "EOF";

    private final LogAppender<Record> appender;

    private final BufferedReader reader;

    public Producer(LogAppender<Record> appender, InputStream inputStream) {
        this.appender = appender;
        reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        try {
            for (; ; ) {
                String line = reader.readLine();
                if (line == null || EOF.equals(line)) {
                    log.debug("End of file, terminating producer");
                    return;
                }
                Record record = buildRecord(line);
                LogOffset offset = appender.append(record.getKey(), record);
                log.debug("Appended offset: {} {} ", offset, record);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            // Exception in an executor are catched make sure they are logged
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private Record buildRecord(String line) {
        String[] keyValue = line.split("\\s+", 2);
        if (keyValue.length == 2) {
            return Record.of(keyValue[0].trim(), keyValue[1].getBytes(StandardCharsets.UTF_8));
        }
        return Record.of(line.trim(), null);
    }
}
