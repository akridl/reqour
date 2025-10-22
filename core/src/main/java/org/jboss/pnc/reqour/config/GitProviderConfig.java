/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.reqour.config;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

/**
 * General configuration for any git provider.
 */
public interface GitProviderConfig {

    String url();

    String username();

    String hostname();

    @WithName("workspace")
    String workspaceName();

    long workspaceId();

    String readOnlyTemplate();

    String readWriteTemplate();

    String gitUrlInternalTemplate();

    String token();

    @WithParentName
    TagProtectionConfig tagProtection();

    interface TagProtectionConfig {

        Optional<String> protectedTagsPattern();

        Optional<List<String>> protectedTagsAcceptedPatterns();
    }
}
