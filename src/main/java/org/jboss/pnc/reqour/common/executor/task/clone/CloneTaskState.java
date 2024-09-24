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

import org.jboss.pnc.reqour.common.utils.IOUtils;
import org.jboss.pnc.reqour.model.ProcessContext;

import java.nio.file.Path;
import java.util.Collections;

/**
 * TODO
 */
public abstract class CloneTaskState {

    protected final CloneTask cloneTask;
    protected final Path cloneDirectory = IOUtils.createTempDirForCloning();
    protected String adjustedUrl;
    protected boolean isInternalRepoNew;
    protected final ProcessContext.Builder processContextBuilder = ProcessContext.builder()
            .workingDirectory(cloneDirectory)
            .extraEnvVariables(Collections.emptyMap())
            .stdoutConsumer(System.out::println)
            .stderrConsumer(System.err::println);
    protected final String targetRemote = "target";

    public CloneTaskState(CloneTask cloneTask) {
        this.cloneTask = cloneTask;
    }

    public abstract void runNextStep();
}
