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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import org.jboss.pnc.api.reqour.dto.RepositoryCloneRequest;
import org.jboss.pnc.api.reqour.dto.RepositoryCloneResponse;
import org.jboss.pnc.reqour.common.GitCommands;
import org.jboss.pnc.reqour.common.executor.task.AbstractTask;
import org.jboss.pnc.reqour.common.executor.task.RunnableTask;
import org.jboss.pnc.reqour.config.ConfigUtils;

/**
 * TODO
 */
@ApplicationScoped
public class CloneTask extends AbstractTask<RepositoryCloneResponse> implements RunnableTask<RepositoryCloneResponse> {

    @Getter
    private final ConfigUtils configUtils;

    @Getter
    private final GitCommands gitCommands;

    @Getter
    @Setter
    private RepositoryCloneRequest request;

    @Setter
    private CloneTaskState cloneState;

    @Inject
    public CloneTask(ConfigUtils configUtils, GitCommands gitCommands) {
        super();
        this.configUtils = configUtils;
        this.gitCommands = gitCommands;
        cloneState = new InitialState(this);
    }

    @Override
    public void runNextStep() {
        cloneState.runNextStep();
    }
}
