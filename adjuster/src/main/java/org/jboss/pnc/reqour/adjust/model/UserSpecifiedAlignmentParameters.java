/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.reqour.adjust.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

/**
 * User specified alignment parameters, i.e., parameters provided in
 * {@link org.jboss.pnc.api.constants.BuildConfigurationParameterKeys#ALIGNMENT_PARAMETERS}.
 */
@Builder
@Value
public class UserSpecifiedAlignmentParameters {

    @Getter
    private static final Optional<Path> defaultLocation = Optional.empty();

    /**
     * Location option: -f/--file for maven builds, -t/--target for gradle builds.<br/>
     * Parsed location option, e.g. specified by "--file=h2/pom.xml" for {@link org.jboss.pnc.api.enums.BuildType#MVN}
     * builds.<br/>
     * TODO in 3.3.0: Remove this field and extract the functionality into corresponding
     * {@link org.jboss.pnc.reqour.adjust.provider.AdjustProvider}s.
     */
    Optional<Path> locationOption;

    /**
     * TODO in 3.3.0: All the user-specified alignment parameters, including the location parameter.<br/>
     * All the remaining user-specified alignment parameters.
     */
    List<String> alignmentParameters;

    public static UserSpecifiedAlignmentParameters defaultResult() {
        return UserSpecifiedAlignmentParameters.withoutLocation(Collections.emptyList());
    }

    public static UserSpecifiedAlignmentParameters withoutLocation(List<String> alignmentParameters) {
        return UserSpecifiedAlignmentParameters.builder()
                .locationOption(defaultLocation)
                .alignmentParameters(alignmentParameters)
                .build();
    }
}
