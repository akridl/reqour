/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014-2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.reqour.common.executor.task.clone;

import lombok.extern.slf4j.Slf4j;
import org.jboss.pnc.api.enums.ResultStatus;
import org.jboss.pnc.api.reqour.dto.RepositoryCloneResponse;
import org.jboss.pnc.api.reqour.dto.ReqourCallback;
import org.jboss.pnc.reqour.common.utils.IOUtils;

import java.io.IOException;

/**
 * TODO
 */
@Slf4j
public class PrepareResultState extends CloneTaskState {

    public PrepareResultState(CloneTask cloneTask) {
        super(cloneTask);
    }

    @Override
    public void runNextStep() {
        try {
            IOUtils.deleteTempDir(cloneDirectory);
        } catch (IOException ex) {
            log.warn("Could not delete the temporary directory", ex);
        }

        cloneTask.setFinished(true);
        cloneTask.setResult(RepositoryCloneResponse.builder()
                .originRepoUrl(cloneTask.getRequest().getOriginRepoUrl())
                .targetRepoUrl(cloneTask.getRequest().getTargetRepoUrl())
                .callback(ReqourCallback.builder().id(cloneTask.getRequest().getTaskId()).status(ResultStatus.SUCCESS).build())
                .build());
    }
}
