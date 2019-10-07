/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.BULK_SCROLL_KEEP_ALIVE_PROPERTY;
import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.BULK_SCROLL_PRODUCE_IMMEDIATE_PROPERTY;
import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.BULK_SCROLL_SIZE_PROPERTY;
import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.DEFAULT_SCROLL_KEEP_ALIVE;
import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.DEFAULT_SCROLL_SIZE;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.COMMAND_STREAM;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.bulk.BulkAdminService;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

public class MyStreamProcessor implements StreamProcessorTopology {
    public static final String MY_SCROLLER = "myScroller";

    private static final Log log = LogFactory.getLog(MyStreamProcessor.class);

    @Override
    public Topology getTopology(Map<String, String> options) {
        BulkAdminService bulkAdminService = Framework.getService(BulkAdminService.class);
        ConfigurationService confService = Framework.getService(ConfigurationService.class);

        List<String> actions = bulkAdminService.getActions();
        int scrollSize = confService.getInteger(BULK_SCROLL_SIZE_PROPERTY, DEFAULT_SCROLL_SIZE);
        int scrollKeepAlive = confService.getInteger(BULK_SCROLL_KEEP_ALIVE_PROPERTY, DEFAULT_SCROLL_KEEP_ALIVE);
        boolean scrollProduceImmediate = confService.isBooleanTrue(BULK_SCROLL_PRODUCE_IMMEDIATE_PROPERTY);

        List<String> mapping = new ArrayList<>();
        mapping.add(INPUT_1 + ":" + COMMAND_STREAM);
        int i = 1;
        for (String action : actions) {
            mapping.add(String.format("o%s:%s", i, action));
            i++;

        }
        mapping.add(String.format("o%s:%s", i, STATUS_STREAM));
        return Topology.builder()
                       .addComputation(() -> new MyBulkScrollerComputation(MY_SCROLLER, actions.size() + 1, scrollSize,
                               scrollKeepAlive, scrollProduceImmediate), mapping)
                       .build();
    }

}
