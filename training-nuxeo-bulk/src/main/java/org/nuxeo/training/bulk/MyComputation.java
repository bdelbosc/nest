package org.nuxeo.training.bulk;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.runtime.api.Framework;

public class MyComputation extends AbstractBulkComputation {

    private static final Logger log = LogManager.getLogger(MyComputation.class);

    protected static final String ACTION_NAME = "myAction";

    public MyComputation() {
        super(ACTION_NAME);
    }

    @Override
    protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
        TagService tagService = Framework.getService(TagService.class);
        String fieldName = (String) properties.get("fieldName");
        if (fieldName == null) {
            log.warn("Missing fieldName params on command: {}", getCurrentCommand());
            return;
        }
        for (DocumentModel doc : loadDocuments(session, ids)) {
            List<String> docTags = getDocTags(doc, fieldName);
            if (!docTags.isEmpty()) {
                docTags.forEach(tag -> tagService.tag(session, doc.getId(), tag));
                log.debug("Updating doc id: {} with tags: {}", doc.getName(), docTags);
            }
        }
    }

    protected List<String> getDocTags(DocumentModel doc, String field) {
        try {
            String[] ret = (String[]) doc.getPropertyValue(field);
            if (ret == null) {
                return Collections.emptyList();
            }
            log.debug("get tags from {} on {}", field, doc.getId());
            return Arrays.asList(ret);
        } catch (PropertyNotFoundException e) {
            return Collections.emptyList();
        }
    }
}
