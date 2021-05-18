/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.operator.cluster.model;

import io.strimzi.operator.common.Reconciliation;
import io.strimzi.operator.common.ReconciliationLogger;
import io.strimzi.operator.common.model.OrderedProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Abstract class for processing and generating configuration passed by the user.
 */
public abstract class AbstractConfiguration {
    private static final Logger LOGGER = LogManager.getLogger(AbstractConfiguration.class.getName());
    private static final ReconciliationLogger RECONCILIATION_LOGGER = new ReconciliationLogger(LOGGER);

    private final OrderedProperties options = new OrderedProperties();

    /**
     * Constructor used to instantiate this class from String configuration. Should be used to create configuration
     * from the Assembly.
     *
     * @param reconciliation    The reconciliation
     * @param configuration     Configuration in String format. Should contain zero or more lines with with key=value
     *                          pairs.
     * @param forbiddenPrefixes List with configuration key prefixes which are not allowed. All keys which start with one of
     *                          these prefixes will be ignored.
     */
    public AbstractConfiguration(Reconciliation reconciliation, String configuration, List<String> forbiddenPrefixes) {
        options.addStringPairs(configuration);
        filterForbidden(reconciliation, forbiddenPrefixes);
    }

    /**
     * Constructor used to instantiate this class from String configuration. Should be used to create configuration
     * from the Assembly.
     *
     * @param reconciliation    The reconciliation
     * @param configuration     Configuration in String format. Should contain zero or more lines with with key=value
     *                          pairs.
     * @param forbiddenPrefixes List with configuration key prefixes which are not allowed. All keys which start with one of
     *                          these prefixes will be ignored.
     * @param defaults          Properties object with default options
     */
    public AbstractConfiguration(Reconciliation reconciliation, String configuration, List<String> forbiddenPrefixes, Map<String, String> defaults) {
        options.addMapPairs(defaults);
        options.addStringPairs(configuration);
        filterForbidden(reconciliation, forbiddenPrefixes);
    }

    /**
     * Constructor used to instantiate this class from JsonObject. Should be used to create configuration from
     * ConfigMap / CRD.
     *
     * @param reconciliation  The reconciliation
     * @param jsonOptions     Json object with configuration options as key ad value pairs.
     * @param forbiddenPrefixes   List with configuration key prefixes which are not allowed. All keys which start with one of
     *                           these prefixes will be ignored.
     */
    public AbstractConfiguration(Reconciliation reconciliation, Iterable<Map.Entry<String, Object>> jsonOptions, List<String> forbiddenPrefixes) {
        options.addIterablePairs(jsonOptions);
        filterForbidden(reconciliation, forbiddenPrefixes);
    }

    /**
     * Constructor used to instantiate this class from JsonObject. Should be used to create configuration from
     * ConfigMap / CRD.
     *
     * @param reconciliation  The reconciliation
     * @param jsonOptions     Json object with configuration options as key ad value pairs.
     * @param forbiddenPrefixes  List with configuration key prefixes which are not allowed. All keys which start with one of
     *                           these prefixes will be ignored.
     * @param forbiddenPrefixExceptions Exceptions excluded from forbidden prefix options checking
     */
    public AbstractConfiguration(Reconciliation reconciliation, Iterable<Map.Entry<String, Object>> jsonOptions, List<String> forbiddenPrefixes, List<String> forbiddenPrefixExceptions) {
        options.addIterablePairs(jsonOptions);
        filterForbidden(reconciliation, forbiddenPrefixes, forbiddenPrefixExceptions);
    }

    /**
     * Constructor used to instantiate this class from JsonObject. Should be used to create configuration from
     * ConfigMap / CRD.
     *
     * @param reconciliation  The reconciliation
     * @param jsonOptions     Json object with configuration options as key ad value pairs.
     * @param forbiddenPrefixes   List with configuration key prefixes which are not allowed. All keys which start with one of
     *                           these prefixes will be ignored.
     * @param defaults          Properties object with default options
     */
    public AbstractConfiguration(Reconciliation reconciliation, Iterable<Map.Entry<String, Object>> jsonOptions, List<String> forbiddenPrefixes, Map<String, String> defaults) {
        options.addMapPairs(defaults);
        options.addIterablePairs(jsonOptions);
        filterForbidden(reconciliation, forbiddenPrefixes);
    }

    /**
     * Constructor used to instantiate this class from JsonObject. Should be used to create configuration from
     * ConfigMap / CRD.
     *
     * @param reconciliation  The reconciliation
     * @param jsonOptions     Json object with configuration options as key ad value pairs.
     * @param forbiddenPrefixes  List with configuration key prefixes which are not allowed. All keys which start with one of
     *                           these prefixes will be ignored.
     * @param forbiddenPrefixExceptions  Exceptions excluded from forbidden prefix options checking
     * @param defaults          Properties object with default options
     */
    public AbstractConfiguration(Reconciliation reconciliation, Iterable<Map.Entry<String, Object>> jsonOptions, List<String> forbiddenPrefixes, List<String> forbiddenPrefixExceptions, Map<String, String> defaults) {
        options.addMapPairs(defaults);
        options.addIterablePairs(jsonOptions);
        filterForbidden(reconciliation, forbiddenPrefixes, forbiddenPrefixExceptions);
    }

    /**
     * Filters forbidden values from the configuration.
     *
     * @param reconciliation    The reconciliation
     * @param forbiddenPrefixes List with configuration key prefixes which are not allowed. All keys which start with one of
     *                          these prefixes will be ignored.
     * @param forbiddenPrefixExceptions Exceptions excluded from forbidden prefix options checking
     */
    private void filterForbidden(Reconciliation reconciliation, List<String> forbiddenPrefixes, List<String> forbiddenPrefixExceptions)   {
        options.filter(k -> forbiddenPrefixes.stream().anyMatch(s -> {
            boolean forbidden = k.toLowerCase(Locale.ENGLISH).startsWith(s);
            if (forbidden) {
                if (forbiddenPrefixExceptions.contains(k))
                    forbidden = false;
            }
            if (forbidden) {
                RECONCILIATION_LOGGER.warn(reconciliation, "Configuration option \"{}\" is forbidden and will be ignored", k);
            } else {
                RECONCILIATION_LOGGER.trace(reconciliation, "Configuration option \"{}\" is allowed and will be passed to the assembly", k);
            }
            return forbidden;
        }));
    }

    private void filterForbidden(Reconciliation reconciliation, List<String> forbiddenPrefixes)   {
        this.filterForbidden(reconciliation, forbiddenPrefixes, Collections.emptyList());
    }

    public String getConfigOption(String configOption) {
        return options.asMap().get(configOption);
    }

    public String getConfigOption(String configOption, String defaultValue) {
        return options.asMap().getOrDefault(configOption, defaultValue);
    }

    public void setConfigOption(String configOption, String value) {
        options.asMap().put(configOption, value);
    }

    public void removeConfigOption(String configOption) {
        options.asMap().remove(configOption);
    }

    /**
     * Generate configuration file in String format.
     *
     * @return  String with one or more lines containing key=value pairs with the configuration options.
     */
    public String getConfiguration() {
        return options.asPairs();
    }

    /**
     * Get access to underlying key-value pairs.  Any changes to OrderedProperties will be reflected in
     * in value returned by subsequent calls to getConfiguration()
     * @return A map of keys to values.
     */
    public OrderedProperties asOrderedProperties() {
        return options;
    }

    /**
     * Splits string with comma-separated values into a List
     *
     * @param prefixes  String with comma-separated items
     * @return          List with the values as separate items
     */
    protected static List<String> splitPrefixesToList(String prefixes) {
        return asList(prefixes.split("\\s*,+\\s*"));
    }
}
