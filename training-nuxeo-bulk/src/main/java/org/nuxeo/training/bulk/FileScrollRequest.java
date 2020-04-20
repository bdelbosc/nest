/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.training.bulk;

import java.nio.file.Path;
import java.util.Objects;

import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

public class FileScrollRequest implements ScrollRequest {
    protected static final String SCROLL_TYPE = "file";

    protected final int size;

    protected final String filePath;

    public FileScrollRequest(Builder builder) {
        this.size = builder.getSize();
        this.filePath = builder.getFilePath();
    }

    @Override
    public String getType() {
        return SCROLL_TYPE;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getSize() {
        return size;
    }

    public String getFilePath() {
        return filePath;
    }

    public static Builder builder(String filePath) {
        return new Builder(filePath);
    }

    @Override
    public String toString() {
        return "FileScrollRequest{" + "size=" + size + ", filePath='" + filePath + '\'' + '}';
    }

    public static class Builder {
        protected final String filePath;

        protected int size;

        public static final int DEFAULT_SCROLL_SIZE = 50;

        protected Builder(String filePath) {
            this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
            if (!Path.of(filePath).toFile().canRead()) {
                throw new IllegalArgumentException("Cannot read file: " + Path.of(filePath).toAbsolutePath());
            }
        }

        public FileScrollRequest.Builder size(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be > 0");
            } else {
                this.size = size;
                return this;
            }
        }

        public String getFilePath() {
            return filePath;
        }

        public int getSize() {
            return this.size == 0 ? DEFAULT_SCROLL_SIZE : this.size;
        }

        public FileScrollRequest build() {
            return new FileScrollRequest(this);
        }
    }
}
