/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.reqour.rest.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.api.reqour.dto.CancelRequest;
import org.jboss.pnc.api.reqour.dto.CancelResponse;
import org.jboss.pnc.api.reqour.dto.ReqourCallback;
import org.jboss.pnc.api.reqour.rest.CancelEndpoint;
import org.jboss.pnc.common.http.PNCHttpClient;
import org.jboss.pnc.reqour.config.ConfigUtils;
import org.jboss.pnc.reqour.rest.openshift.OpenShiftAdjusterJobController;

@ApplicationScoped
@Slf4j
public class CancelEndpointImpl implements CancelEndpoint {

    private final ManagedExecutor managedExecutor;
    private final OpenShiftAdjusterJobController openShiftAdjusterJobController;
    private final PNCHttpClient pncHttpClient;

    public CancelEndpointImpl(
            ManagedExecutor managedExecutor,
            OpenShiftAdjusterJobController openShiftAdjusterJobController,
            ObjectMapper objectMapper,
            ConfigUtils configUtils) {
        this.managedExecutor = managedExecutor;
        this.openShiftAdjusterJobController = openShiftAdjusterJobController;
        pncHttpClient = new PNCHttpClient(objectMapper, configUtils.getPncHttpClientConfig());
    }

    @Override
    public void cancelTask(CancelRequest cancelRequest) {
        managedExecutor.supplyAsync(() -> openShiftAdjusterJobController.destroyAdjusterJob(cancelRequest.getTaskId()))
                .exceptionally(t -> {
                    log.error("Cancellation of task with ID '{}' ended unexpectedly", cancelRequest.getTaskId(), t);
                    return ResultStatus.SYSTEM_ERROR;
                })
                .thenAccept(
                        status -> pncHttpClient.sendRequest(
                                cancelRequest.getCallback(),
                                CancelResponse.builder()
                                        .callback(
                                                ReqourCallback.builder()
                                                        .id(cancelRequest.getTaskId())
                                                        .status(status)
                                                        .build())
                                        .build()));

        throw new WebApplicationException(Response.Status.ACCEPTED);
    }
}
