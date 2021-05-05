/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.strimzi.api.kafka.model.Constants;
import io.strimzi.api.kafka.model.UnknownPropertyPreserving;
import io.strimzi.crdgenerator.annotations.Description;
import io.strimzi.crdgenerator.annotations.PresentInVersions;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation of a template for Strimzi internal services.
 * It contains additional values applicable to internal services..
 */
@Buildable(
        editableEnabled = false,
        builderPackage = Constants.FABRIC8_KUBERNETES_API
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"metadata", "ipFamilyPolicy", "ipFamilies"})
@EqualsAndHashCode
public class InternalServiceTemplate implements Serializable, UnknownPropertyPreserving {
    private static final long serialVersionUID = 1L;

    private MetadataTemplate metadata;
    private IpFamilyPolicy ipFamilyPolicy;
    private List<IpFamily> ipFamilies;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @Description("Metadata applied to the resource.")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public MetadataTemplate getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataTemplate metadata) {
        this.metadata = metadata;
    }

    @Description("Specifies which IP Family Policy should be used by this service. " +
            "Available options are `SingleStack` (a single IP family), " +
            "`PreferDualStack` (two IP families on dual-stack configured clusters or a single IP family on single-stack clusters), " +
            "and `RequireDualStack` (two IP families on dual-stack configured clusters, otherwise fail). " +
            "If unspecified, Kubernetes will choose the default value based on the service type. " +
            "Available on Kubernetes 1.20 and newer.")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @PresentInVersions("v1beta2+")
    public IpFamilyPolicy getIpFamilyPolicy() {
        return ipFamilyPolicy;
    }

    public void setIpFamilyPolicy(IpFamilyPolicy ipFamilyPolicy) {
        this.ipFamilyPolicy = ipFamilyPolicy;
    }

    @Description("Specifies which IP Families should be used by this service. " +
            "Available options are `IPv4` and `IPv6. " +
            "If unspecified, Kubernetes will choose the default value based on the `ipFamilyPolicy` setting. " +
            "Available on Kubernetes 1.20 and newer.")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @PresentInVersions("v1beta2+")
    public List<IpFamily> getIpFamilies() {
        return ipFamilies;
    }

    public void setIpFamilies(List<IpFamily> ipFamilies) {
        this.ipFamilies = ipFamilies;
    }

    @Override
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @Override
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
