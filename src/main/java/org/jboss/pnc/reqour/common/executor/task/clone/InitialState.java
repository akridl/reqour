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
import org.jboss.pnc.reqour.common.utils.IOUtils;
import org.jboss.pnc.reqour.common.utils.URLUtils;
import org.jboss.pnc.reqour.model.ProcessContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

/**
 * TODO
 */
@Slf4j
public class InitialState extends CloneTaskState {

    public InitialState(CloneTask cloneTask) {
        super(cloneTask);
    }

    @Override
    public void runNextStep() {
        adjustedUrl = URLUtils.addUsernameToUrl(cloneTask.getRequest().getTargetRepoUrl(), cloneTask.getConfigUtils().getActiveGitBackend().username());
        isInternalRepoNew = isInternalRepoNew(adjustedUrl);
        log.info("Internal repository with adjusted URL '{}' is considered new: {}", adjustedUrl, isInternalRepoNew);

        if (cloneTask.getRequest().getRef() == null || isInternalRepoNew) {
            cloneTask.setCloneState(new CloneMirrorState(cloneTask));
        } else {
            // cloneTask.setCloneState();
        }
    }

    private boolean isInternalRepoNew(String url) {
        log.info("Checking if internal repository with url '{}' is new", url);

        Path cloneDir = IOUtils.createTempDirForCloning();
        ProcessContext.Builder processContextBuilder = ProcessContext.builder()
                .workingDirectory(cloneDir)
                .extraEnvVariables(Collections.emptyMap())
                .stdoutConsumer(System.out::println)
                .stderrConsumer(System.err::println);

        cloneTask.getGitCommands().clone(url, processContextBuilder);
        final boolean result;
        if (IOUtils.countLines(cloneTask.getGitCommands().listTags(processContextBuilder)) > 0) {
            result = false;
        } else {
            result = IOUtils.countLines(cloneTask.getGitCommands().listBranches(processContextBuilder)) == 0;
        }

        try {
            IOUtils.deleteTempDir(cloneDir);
        } catch (IOException e) {
            log.warn("Could not delete the temporary directory", e);
        }

        return result;
    }
}
