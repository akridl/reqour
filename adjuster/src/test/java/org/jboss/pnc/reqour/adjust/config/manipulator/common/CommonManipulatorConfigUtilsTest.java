/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.reqour.adjust.config.manipulator.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.pnc.api.constants.BuildConfigurationParameterKeys;
import org.jboss.pnc.api.reqour.dto.AdjustRequest;
import org.jboss.pnc.reqour.adjust.model.UserSpecifiedAlignmentParameters;
import org.junit.jupiter.api.Test;

class CommonManipulatorConfigUtilsTest {

    @Test
    void parseUserSpecifiedAlignmentParameters_noLocation_returnsParsedAlignmentParametersWithDefaultLocation() {
        AdjustRequest request = AdjustRequest.builder()
                .buildConfigParameters(
                        Map.of(BuildConfigurationParameterKeys.ALIGNMENT_PARAMETERS, "-Dfoo=bar -Dbaz=\"baz baz\""))
                .build();
        UserSpecifiedAlignmentParameters expected = UserSpecifiedAlignmentParameters
                .withoutLocation(List.of("-Dfoo=bar", "-Dbaz=baz baz"));

        UserSpecifiedAlignmentParameters actual = CommonManipulatorConfigUtils
                .parseUserSpecifiedAlignmentParameters(request);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void parseUserSpecifiedAlignmentParameters_withLocation_returnsParsedAlignmentParametersWithLocation() {
        AdjustRequest request = AdjustRequest.builder()
                .buildConfigParameters(
                        Map.of(
                                BuildConfigurationParameterKeys.ALIGNMENT_PARAMETERS,
                                "-Dfoo=bar --file=/tmp/dir/file -Dbaz=\"baz baz\""))
                .build();
        UserSpecifiedAlignmentParameters expected = UserSpecifiedAlignmentParameters.builder()
                .locationOption(Optional.of(Path.of("/tmp/dir"))) // todo: this should fail eventually, and be /tmp/dir/file
                .alignmentParameters(List.of("-Dfoo=bar", "-Dbaz=baz baz"))
                .build();

        UserSpecifiedAlignmentParameters actual = CommonManipulatorConfigUtils
                .parseUserSpecifiedAlignmentParameters(request);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void parseUserSpecifiedAlignmentParameters_customOptionsWithLocation_returnsParsedAlignmentParametersWithLocation() {
        AdjustRequest request = AdjustRequest.builder()
                .buildConfigParameters(
                        Map.of(
                                BuildConfigurationParameterKeys.ALIGNMENT_PARAMETERS,
                                "-Dfoo=bar --target=/tmp/dir -Dbaz=\"baz baz\""))
                .build();
        UserSpecifiedAlignmentParameters expected = UserSpecifiedAlignmentParameters.builder()
                .locationOption(Optional.of(Path.of("/tmp/dir")))
                .alignmentParameters(List.of("-Dfoo=bar", "-Dbaz=baz baz"))
                .build();

        UserSpecifiedAlignmentParameters actual = CommonManipulatorConfigUtils
                .parseUserSpecifiedAlignmentParameters(request, "t", "target");

        assertThat(actual).isEqualTo(expected);
    }
}