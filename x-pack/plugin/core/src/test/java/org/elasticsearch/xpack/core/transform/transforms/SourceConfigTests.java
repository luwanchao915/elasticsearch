/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.core.transform.transforms;

import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.junit.Before;

import java.io.IOException;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

public class SourceConfigTests extends AbstractSerializingTransformTestCase<SourceConfig> {

    private boolean lenient;

    public static SourceConfig randomSourceConfig() {
        return new SourceConfig(
            generateRandomStringArray(10, 10, false, false),
            QueryConfigTests.randomQueryConfig(),
            randomRuntimeMappings());
    }

    public static SourceConfig randomInvalidSourceConfig() {
        // create something broken but with a source
        return new SourceConfig(
            generateRandomStringArray(10, 10, false, false),
            QueryConfigTests.randomInvalidQueryConfig(),
            randomRuntimeMappings());
    }

    private static Map<String, Object> randomRuntimeMappings() {
        return randomList(0, 10, () -> randomAlphaOfLengthBetween(1, 10)).stream()
            .distinct()
            .collect(toMap(f -> f, f -> singletonMap("type", randomFrom("boolean", "date", "double", "keyword", "long"))));
    }

    @Before
    public void setRandomFeatures() {
        lenient = randomBoolean();
    }

    @Override
    protected SourceConfig doParseInstance(XContentParser parser) throws IOException {
        return SourceConfig.fromXContent(parser, lenient);
    }

    @Override
    protected SourceConfig createTestInstance() {
        return lenient ? randomBoolean() ? randomSourceConfig() : randomInvalidSourceConfig() : randomSourceConfig();
    }

    @Override
    protected boolean supportsUnknownFields() {
        return lenient;
    }

    @Override
    protected Predicate<String> getRandomFieldsExcludeFilter() {
        // allow unknown fields in the root of the object only as QueryConfig stores a Map<String, Object>
        return field -> !field.isEmpty();
    }

    @Override
    protected Reader<SourceConfig> instanceReader() {
        return SourceConfig::new;
    }

    public void testRequiresRemoteCluster() {
        assertFalse(new SourceConfig(new String [] {"index1", "index2", "index3"},
                QueryConfigTests.randomQueryConfig(), randomRuntimeMappings()).requiresRemoteCluster());

        assertTrue(new SourceConfig(new String [] {"index1", "remote2:index2", "index3"},
                QueryConfigTests.randomQueryConfig(), randomRuntimeMappings()).requiresRemoteCluster());

        assertTrue(new SourceConfig(new String [] {"index1", "index2", "remote3:index3"},
                QueryConfigTests.randomQueryConfig(), randomRuntimeMappings()).requiresRemoteCluster());

        assertTrue(new SourceConfig(new String [] {"index1", "remote2:index2", "remote3:index3"},
                QueryConfigTests.randomQueryConfig(), randomRuntimeMappings()).requiresRemoteCluster());

        assertTrue(new SourceConfig(new String [] {"remote1:index1"},
                QueryConfigTests.randomQueryConfig(), randomRuntimeMappings()).requiresRemoteCluster());
    }
}
