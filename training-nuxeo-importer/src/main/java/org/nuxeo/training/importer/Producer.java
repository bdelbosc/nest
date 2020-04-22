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
package org.nuxeo.training.importer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogOffset;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class Producer implements Runnable {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(Producer.class);

    private final LogAppender<DocumentMessage> appender;

    private final JsonParser jp;

    private int MAX_SIZE = 2000;

    public Producer(LogAppender<DocumentMessage> appender, String filePath) {
        this.appender = appender;
        JsonFactory f = new MappingJsonFactory();
        try {
            jp = f.createParser(new File(filePath));
            if (jp.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalArgumentException("Invalid JSON file: " + filePath + " expecting JSON ARRAY");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON file: " + filePath, e);
        }
    }

    @Override
    public void run() {
        try {
            while (jp.nextToken() != JsonToken.END_ARRAY) {
                JsonNode style = jp.readValueAsTree();
                String name = getValue(style, "name");
                String styleId = String.format("%02d", style.get("id").asInt());
                if ("00".equals(styleId)) {
                    styleId = style.get("id").asText();
                }
                String note = getValue(style, "notes");
                appendStyle(styleId, name, note);
                JsonNode subcat = style.get("subcategories");
                if (subcat != null) {
                    String finalStyleId = styleId;
                    subcat.elements().forEachRemaining(subcategory -> appendSubCategory(finalStyleId, subcategory));
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error: " + e.getMessage(), e);
        }
    }

    private String getValue(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        if (value != null) {
            String ret = value.asText();
            if (ret.length() > MAX_SIZE) {
                log.warn("Truncating field {} to max size: {}", field, MAX_SIZE);
                return ret.substring(0, MAX_SIZE);
            }
            return ret;
        }
        return defaultValue;
    }

    private String getValue(JsonNode node, String field) {
        return getValue(node, field, "");
    }

    private Serializable getValues(JsonNode node, String field) {
        JsonNode elements = node.get(field);
        ArrayList<String> ret = new ArrayList<>();
        if (elements == null) {
            return ret;
        }
        elements.elements().forEachRemaining(element -> ret.add(element.textValue()));
        return ret;
    }

    private void appendSubCategory(String styleId, JsonNode subcat) {
        String subCategoryId = formatSubCat(getValue(subcat, "id"));
        String name = getValue(subcat, "name");
        String desc = getValue(subcat, "overall_impression", "");
        HashMap<String, Serializable> props = new HashMap<>();
        props.put("dc:title", subCategoryId + " " + name);
        props.put("dc:description", desc);
        props.put("bjcp:overall_impression", desc);
        props.put("bjcp:aroma", getValue(subcat, "aroma"));
        props.put("bjcp:appearance", getValue(subcat, "appearance"));
        props.put("bjcp:flavor", getValue(subcat, "flavor"));
        props.put("bjcp:mouthfeel", getValue(subcat, "mouthfeel"));
        props.put("bjcp:comments", getValue(subcat, "comments"));
        props.put("bjcp:history", getValue(subcat, "history"));
        props.put("bjcp:characteristic_ingredients", getValue(subcat, "characteristic_ingredients"));
        props.put("bjcp:style_comparison", getValue(subcat, "style_comparison"));
        props.put("bjcp:commercial_examples", getValues(subcat, "commercial_examples"));
        props.put("bjcp:tags", getValues(subcat, "tags"));
        props.put("bjcp:style", styleId);
        props.put("bjcp:family", getValue(subcat, "family"));
        props.put("bjcp:family_history", getValue(subcat, "family_history"));
        props.put("bjcp:origin", getValue(subcat, "origin"));
        JsonNode vital = subcat.get("vital_statistics");
        if (vital != null) {
            props.put("bjcp:original_extract", getMinMax(vital, "original_extract"));
            props.put("bjcp:terminal_extract", getMinMax(vital, "terminal_extract"));
            props.put("bjcp:alcohol", getMinMax(vital, "alcohol"));
            props.put("bjcp:bitterness", getMinMax(vital, "bitterness"));
            props.put("bjcp:color", getMinMax(vital, "color"));
        }
        DocumentMessage doc = DocumentMessage.builder("subcategory", "/" + styleId, subCategoryId)
                                             .setProperties(props)
                                             .build();
        LogOffset offset = appender.append(styleId, doc);
        log.debug("Appended Sub Category offset: {} {} ", offset, doc);
        JsonNode subSubcat = subcat.get("styles");
        if (subSubcat != null) {
            subSubcat.elements().forEachRemaining(subcategory -> appendSubCategory(styleId, subcategory));
        }
    }

    private String formatSubCat(String name) {
        if (name.matches("[0-9][A-Z].*")) {
            return "0" + name;
        }
        return name;
    }

    private Serializable getMinMax(JsonNode vital, String field) {
        JsonNode node = vital.get(field);
        double min = node.get("min").asDouble();
        double max = node.get("max").asDouble();
        HashMap<String, Double> ret = new HashMap<String, Double>(3);
        ret.put("min", min);
        ret.put("max", max);
        ret.put("avg", (min + max)/2);
        return ret;
    }

    private void appendStyle(String styleId, String name, String note) {
        HashMap<String, Serializable> props = new HashMap<>();
        props.put("dc:title", styleId + " " + name);
        props.put("dc:description", note);
        DocumentMessage doc = DocumentMessage.builder("style", "/", styleId).setProperties(props).build();
        LogOffset offset = appender.append(styleId, doc);
        log.debug("Appended Style offset: {} {} ", offset, doc);
    }

}
