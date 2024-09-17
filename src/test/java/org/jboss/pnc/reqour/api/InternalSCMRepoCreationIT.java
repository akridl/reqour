/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2024-2024 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.reqour.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.utils.UrlEncoder;
import org.jboss.pnc.api.reqour.rest.InternalSCMRepositoryCreationEndpoint;
import org.jboss.pnc.reqour.common.TestDataSupplier;
import org.jboss.pnc.reqour.common.TestUtils;
import org.jboss.pnc.reqour.profile.InternalSCMRepositoryCreationProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.jboss.pnc.api.constants.HttpHeaders.ACCEPT_STRING;
import static org.jboss.pnc.api.constants.HttpHeaders.CONTENT_TYPE_STRING;

@QuarkusTest
@TestHTTPEndpoint(InternalSCMRepositoryCreationEndpoint.class)
@TestProfile(InternalSCMRepositoryCreationProfile.class)
@ConnectWireMock
public class InternalSCMRepoCreationIT {

    private static final String CALLBACK_PATH = "/callback";
    private static final String GITLAB_API_PATH = "/api/v4";

    @Inject
    ObjectMapper objectMapper;

    WireMock wireMock;

    @BeforeEach
    void setUp() {
        wireMock.register(WireMock.post(CALLBACK_PATH).willReturn(WireMock.ok()));
    }

    @AfterEach
    void tearDown() {
        wireMock.resetRequests();
    }

    @Test
    void createInternalSCMRepository_newProjectWithoutSubgroup_createsNewProject()
            throws InterruptedException, JsonProcessingException {
        String projectPath = TestDataSupplier.InternalSCM.WORKSPACE_NAME + "/"
                + TestDataSupplier.InternalSCM.PROJECT_NAME;
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/groups/" + TestDataSupplier.InternalSCM.WORKSPACE_ID,
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.workspaceGroup()));
        wireMock.register(
                WireMock.get(GITLAB_API_PATH + "/projects/" + UrlEncoder.urlEncode(projectPath))
                        .willReturn(
                                WireMock.notFound()
                                        .withBody(
                                                objectMapper.writeValueAsString(
                                                        new GitLabApiException("Project not found")))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));
        wireMock.register(
                WireMock.post(GITLAB_API_PATH + "/projects")
                        .withFormParam(
                                "namespace_id",
                                WireMock.equalTo(String.valueOf(TestDataSupplier.InternalSCM.WORKSPACE_ID)))
                        .withFormParam("name", WireMock.equalTo(TestDataSupplier.InternalSCM.PROJECT_NAME))
                        .withHeader(CONTENT_TYPE_STRING, WireMock.equalTo(MediaType.APPLICATION_FORM_URLENCODED))
                        .withHeader(ACCEPT_STRING, WireMock.equalTo(MediaType.APPLICATION_JSON))
                        .willReturn(
                                WireMock.ok(
                                        objectMapper.writeValueAsString(
                                                TestDataSupplier.InternalSCM.projectFromTestWorkspace()))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/projects/" + TestDataSupplier.InternalSCM.PROJECT_ID
                        + "/protected_tags?page=1&per_page=100",
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.protectedTags()));
        wireMock.register(
                WireMock.post(
                        GITLAB_API_PATH + "/projects/" + TestDataSupplier.InternalSCM.PROJECT_ID + "/protected_tags")
                        .withFormParam(
                                "name",
                                WireMock.equalTo(TestDataSupplier.InternalSCM.createdProtectedTag().getName()))
                        .withHeader(CONTENT_TYPE_STRING, WireMock.equalTo(MediaType.APPLICATION_FORM_URLENCODED))
                        .withHeader(ACCEPT_STRING, WireMock.equalTo(MediaType.APPLICATION_JSON))
                        .willReturn(
                                WireMock.ok(
                                        objectMapper
                                                .writeValueAsString(TestDataSupplier.InternalSCM.createdProtectedTag()))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));

        given().when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        TestUtils.createInternalSCMRepoCreationRequest(
                                TestDataSupplier.InternalSCM.PROJECT_NAME,
                                TestDataSupplier.TASK_ID,
                                CALLBACK_PATH))
                .when()
                .post()
                .then()
                .statusCode(202);

        Thread.sleep(1_500);
        TestUtils.verifyThatCallbackWasSent(
                wireMock,
                CALLBACK_PATH,
                objectMapper.writeValueAsString(TestUtils.newlyCreatedSuccess(projectPath, TestDataSupplier.TASK_ID)));
    }

    @Test
    void createInternalSCMRepository_alreadyExistingProjectWithoutSubgroup_successWithAlreadyExists()
            throws InterruptedException, JsonProcessingException {
        String projectPath = TestDataSupplier.InternalSCM.WORKSPACE_NAME + "/"
                + TestDataSupplier.InternalSCM.PROJECT_NAME;
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/groups/" + TestDataSupplier.InternalSCM.WORKSPACE_ID,
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.workspaceGroup()));
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/projects/" + UrlEncoder.urlEncode(projectPath),
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.projectFromTestWorkspace()));
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/projects/" + TestDataSupplier.InternalSCM.PROJECT_ID
                        + "/protected_tags?page=1&per_page=100",
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.protectedTags()));

        given().when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        TestUtils.createInternalSCMRepoCreationRequest(
                                "project",
                                TestDataSupplier.TASK_ID,
                                CALLBACK_PATH))
                .when()
                .post()
                .then()
                .statusCode(202);

        Thread.sleep(1_500);
        TestUtils.verifyThatCallbackWasSent(
                wireMock,
                CALLBACK_PATH,
                objectMapper.writeValueAsString(TestUtils.alreadyExistsSuccess(projectPath, TestDataSupplier.TASK_ID)));
    }

    @Test
    void createInternalSCMRepository_newProjectWithSubgroupSameToWorkspace_createsNewProject()
            throws InterruptedException, JsonProcessingException {
        String projectPath = TestDataSupplier.InternalSCM.WORKSPACE_NAME + "/"
                + TestDataSupplier.InternalSCM.PROJECT_NAME;
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/groups/" + TestDataSupplier.InternalSCM.WORKSPACE_ID,
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.workspaceGroup()));
        wireMock.register(
                WireMock.get(GITLAB_API_PATH + "/projects/" + UrlEncoder.urlEncode(projectPath))
                        .willReturn(
                                WireMock.notFound()
                                        .withBody(
                                                objectMapper.writeValueAsString(
                                                        new GitLabApiException("Project not found")))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));
        wireMock.register(
                WireMock.post(GITLAB_API_PATH + "/projects")
                        .withFormParam(
                                "namespace_id",
                                WireMock.equalTo(String.valueOf(TestDataSupplier.InternalSCM.WORKSPACE_ID)))
                        .withFormParam("name", WireMock.equalTo(TestDataSupplier.InternalSCM.PROJECT_NAME))
                        .withHeader(CONTENT_TYPE_STRING, WireMock.equalTo(MediaType.APPLICATION_FORM_URLENCODED))
                        .withHeader(ACCEPT_STRING, WireMock.equalTo(MediaType.APPLICATION_JSON))
                        .willReturn(
                                WireMock.ok(
                                        objectMapper.writeValueAsString(
                                                TestDataSupplier.InternalSCM.projectFromTestWorkspace()))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/projects/" + TestDataSupplier.InternalSCM.PROJECT_ID
                        + "/protected_tags?page=1&per_page=100",
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.protectedTags()));
        wireMock.register(
                WireMock.post(
                        GITLAB_API_PATH + "/projects/" + TestDataSupplier.InternalSCM.PROJECT_ID + "/protected_tags")
                        .withFormParam(
                                "name",
                                WireMock.equalTo(TestDataSupplier.InternalSCM.createdProtectedTag().getName()))
                        .withHeader(CONTENT_TYPE_STRING, WireMock.equalTo(MediaType.APPLICATION_FORM_URLENCODED))
                        .withHeader(ACCEPT_STRING, WireMock.equalTo(MediaType.APPLICATION_JSON))
                        .willReturn(
                                WireMock.ok(
                                        objectMapper
                                                .writeValueAsString(TestDataSupplier.InternalSCM.createdProtectedTag()))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));

        given().when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        TestUtils.createInternalSCMRepoCreationRequest(
                                projectPath,
                                TestDataSupplier.TASK_ID,
                                CALLBACK_PATH))
                .when()
                .post()
                .then()
                .statusCode(202);

        Thread.sleep(1_500);
        TestUtils.verifyThatCallbackWasSent(
                wireMock,
                CALLBACK_PATH,
                objectMapper.writeValueAsString(TestUtils.newlyCreatedSuccess(projectPath, TestDataSupplier.TASK_ID)));
    }

    @Test
    void createInternalSCMRepository_alreadyExistingProjectWithSubgroupDifferentFromWorkspace_successWithAlreadyExists()
            throws InterruptedException, JsonProcessingException {
        String projectPath = TestDataSupplier.InternalSCM.DIFFERENT_WORKSPACE_NAME + "/"
                + TestDataSupplier.InternalSCM.PROJECT_NAME;
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/groups/" + TestDataSupplier.InternalSCM.WORKSPACE_ID,
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.workspaceGroup()));
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/groups/"
                        + UrlEncoder.urlEncode(
                                TestDataSupplier.InternalSCM.WORKSPACE_NAME + "/"
                                        + TestDataSupplier.InternalSCM.DIFFERENT_WORKSPACE_NAME),
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.differentWorkspaceGroup()));
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/projects/"
                        + UrlEncoder.urlEncode(
                                TestDataSupplier.InternalSCM.projectFromDifferentWorkspace().getPathWithNamespace()),
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.projectFromDifferentWorkspace()));
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/projects/" + TestDataSupplier.InternalSCM.DIFFERENT_PROJECT_ID
                        + "/protected_tags?page=1&per_page=100",
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.protectedTags()));

        given().when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        TestUtils.createInternalSCMRepoCreationRequest(
                                projectPath,
                                TestDataSupplier.TASK_ID,
                                CALLBACK_PATH))
                .when()
                .post()
                .then()
                .statusCode(202);

        Thread.sleep(1_500);
        TestUtils.verifyThatCallbackWasSent(
                wireMock,
                CALLBACK_PATH,
                objectMapper.writeValueAsString(
                        TestUtils.alreadyExistsSuccess(
                                TestDataSupplier.InternalSCM.projectFromDifferentWorkspace().getPathWithNamespace(),
                                TestDataSupplier.TASK_ID)));
    }

    @Test
    void createInternalSCMRepository_newProjectWithSubgroupDifferentFromWorkspace_createsNewProjectAndSubgroup()
            throws JsonProcessingException, InterruptedException {
        String projectPath = TestDataSupplier.InternalSCM.DIFFERENT_WORKSPACE_NAME + "/"
                + TestDataSupplier.InternalSCM.PROJECT_NAME;
        TestUtils.registerGet(
                wireMock,
                GITLAB_API_PATH + "/groups/" + TestDataSupplier.InternalSCM.WORKSPACE_ID,
                objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.workspaceGroup()));
        wireMock.register(
                WireMock.get(
                        GITLAB_API_PATH + "/groups/"
                                + UrlEncoder.urlEncode(
                                        TestDataSupplier.InternalSCM.WORKSPACE_NAME + "/"
                                                + TestDataSupplier.InternalSCM.DIFFERENT_WORKSPACE_NAME))
                        .willReturn(
                                WireMock.notFound()
                                        .withBody(
                                                objectMapper.writeValueAsString(
                                                        new GitLabApiException("Group not found")))));
        wireMock.register(
                WireMock.post(GITLAB_API_PATH + "/groups")
                        .withHeader(CONTENT_TYPE_STRING, WireMock.equalTo(MediaType.APPLICATION_FORM_URLENCODED))
                        .withHeader(ACCEPT_STRING, WireMock.equalTo(MediaType.APPLICATION_JSON))
                        .withFormParam("name", WireMock.equalTo(TestDataSupplier.InternalSCM.DIFFERENT_WORKSPACE_NAME))
                        .willReturn(
                                WireMock.ok(
                                        objectMapper.writeValueAsString(
                                                TestDataSupplier.InternalSCM.differentWorkspaceGroup()))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));
        wireMock.register(
                WireMock.get(GITLAB_API_PATH + "/projects/" + UrlEncoder.urlEncode(projectPath))
                        .willReturn(
                                WireMock.notFound()
                                        .withBody(
                                                objectMapper.writeValueAsString(
                                                        new GitLabApiException("Project not found")))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));
        wireMock.register(
                WireMock.post(GITLAB_API_PATH + "/projects")
                        .withFormParam(
                                "namespace_id",
                                WireMock.equalTo(String.valueOf(TestDataSupplier.InternalSCM.DIFFERENT_WORKSPACE_ID)))
                        .withFormParam(
                                "name",
                                WireMock.equalTo(
                                        TestDataSupplier.InternalSCM.projectFromDifferentWorkspace().getName()))
                        .withHeader(ACCEPT_STRING, WireMock.equalTo(MediaType.APPLICATION_JSON))
                        .withHeader(CONTENT_TYPE_STRING, WireMock.equalTo(MediaType.APPLICATION_FORM_URLENCODED))
                        .willReturn(
                                WireMock.ok(
                                        objectMapper.writeValueAsString(
                                                TestDataSupplier.InternalSCM.projectFromDifferentWorkspace()))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));
        wireMock.register(
                WireMock.post(
                        GITLAB_API_PATH + "/projects/" + TestDataSupplier.InternalSCM.DIFFERENT_PROJECT_ID
                                + "/protected_tags")
                        .withFormParam(
                                "name",
                                WireMock.equalTo(TestDataSupplier.InternalSCM.createdProtectedTag().getName()))
                        .withHeader(CONTENT_TYPE_STRING, WireMock.equalTo(MediaType.APPLICATION_FORM_URLENCODED))
                        .withHeader(ACCEPT_STRING, WireMock.equalTo(MediaType.APPLICATION_JSON))
                        .willReturn(
                                WireMock.ok(
                                        objectMapper
                                                .writeValueAsString(TestDataSupplier.InternalSCM.createdProtectedTag()))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));

        given().when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        TestUtils.createInternalSCMRepoCreationRequest(
                                projectPath,
                                TestDataSupplier.TASK_ID,
                                CALLBACK_PATH))
                .when()
                .post()
                .then()
                .statusCode(202);

        Thread.sleep(1_500);
        TestUtils.verifyThatCallbackWasSent(
                wireMock,
                CALLBACK_PATH,
                objectMapper.writeValueAsString(
                        TestUtils.newlyCreatedSuccess(
                                TestDataSupplier.InternalSCM.projectFromDifferentWorkspace().getPathWithNamespace(),
                                TestDataSupplier.TASK_ID)));
    }

    @Test
    void createInternalSCMRepository_invalidRequestWithTooDeepHierarchy_sendsFailInCallback()
            throws JsonProcessingException, InterruptedException {
        wireMock.register(
                WireMock.get(GITLAB_API_PATH + "/groups/" + TestDataSupplier.InternalSCM.WORKSPACE_ID)
                        .willReturn(
                                WireMock.ok(
                                        objectMapper.writeValueAsString(TestDataSupplier.InternalSCM.workspaceGroup()))
                                        .withHeader(CONTENT_TYPE_STRING, MediaType.APPLICATION_JSON)));
        String projectPath = "group/subgroup/project";
        String expectedBody = objectMapper.writeValueAsString(
                TestUtils.failed(
                        TestDataSupplier.InternalSCM.WORKSPACE_NAME + "/" + projectPath,
                        TestDataSupplier.TASK_ID));

        given().when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        TestUtils.createInternalSCMRepoCreationRequest(
                                projectPath,
                                TestDataSupplier.TASK_ID,
                                CALLBACK_PATH))
                .when()
                .post()
                .then()
                .statusCode(202);

        Thread.sleep(1_500);
        TestUtils.verifyThatCallbackWasSent(wireMock, CALLBACK_PATH, expectedBody);
    }
}
