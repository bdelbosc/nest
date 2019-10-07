package org.nuxeo.training.bulk;

import static org.nuxeo.ecm.core.bulk.BulkAdminServiceImpl.STOP_DURATION;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.BULK_LOG_MANAGER_NAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.RECORD_CODEC;

import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.stream.StreamService;

public class MyComponent extends DefaultComponent {

    private StreamProcessor streamProcessor;

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        // Set a processor
        StreamService service = Framework.getService(StreamService.class);
        StreamManager streamManager = service.getStreamManager(BULK_LOG_MANAGER_NAME);
        CodecService codecService = Framework.getService(CodecService.class);
        Codec<Record> codec = codecService.getCodec(RECORD_CODEC, Record.class);
        Settings settings = new Settings(1, 1, codec);
        System.out.println("Init my processor");
        streamProcessor = streamManager.registerAndCreateProcessor("myProcessor",
                new MyStreamProcessor().getTopology(null), settings);
        streamProcessor.start();

    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        if (streamProcessor != null) {
            System.out.println("Shutdown processor");
            streamProcessor.stop(STOP_DURATION);
        }
    }
}
