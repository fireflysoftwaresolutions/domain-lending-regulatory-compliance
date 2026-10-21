/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.domain.lending.compliance.web.controllers;

import io.fireflysoftware.core.lending.compliance.sdk.model.AmlCaseDTO;
import io.fireflysoftware.domain.lending.compliance.core.commands.CreateAmlCaseCommand;
import io.fireflysoftware.domain.lending.compliance.core.commands.UpdateAmlCaseCommand;
import io.fireflysoftware.domain.lending.compliance.core.commands.UpdateAmlCaseStatusCommand;
import io.fireflysoftware.domain.lending.compliance.core.services.AmlCasesService;
import io.fireflysoftware.domain.lending.compliance.interfaces.dtos.ReasonRequest;
import io.fireflysoftware.domain.lending.compliance.interfaces.dtos.StatusChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Domain API for AML cases.
 */
@RestController
@RequestMapping("/api/v1/aml-cases")
@Tag(name = "AmlCases", description = "Domain orchestration for AML cases")
public class AmlCasesController {

    private final AmlCasesService service;

    public AmlCasesController(AmlCasesService service) {
        this.service = service;
    }

    @Operation(summary = "Create AML case", description = "Runs the register saga against core-lending-regulatory-compliance.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AmlCaseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<AmlCaseDTO>> create(@RequestBody AmlCaseDTO body) {
        return service.create(new CreateAmlCaseCommand(body))
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @Operation(summary = "Get AML case", description = "Reads one record from core-lending-regulatory-compliance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AmlCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @GetMapping(value = "/{amlCaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<AmlCaseDTO>> get(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID amlCaseId) {
        return service.get(amlCaseId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update status", description = "Runs the status saga against core-lending-regulatory-compliance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AmlCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{amlCaseId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<AmlCaseDTO>> updateStatus(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID amlCaseId,
            @RequestBody StatusChangeRequest request) {
        return service.updateStatus(new UpdateAmlCaseStatusCommand(amlCaseId, request.getStatus(), request.getStatusReason()))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Update AML case", description = "Runs the update saga against core-lending-regulatory-compliance. Status is left unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AmlCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PutMapping(value = "/{amlCaseId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<AmlCaseDTO>> update(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID amlCaseId,
            @RequestBody AmlCaseDTO body) {
        return service.update(new UpdateAmlCaseCommand(amlCaseId, body))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Close AML case", description = "Closes an AML case by writing CLOSED when the regulatory compliance core allows that status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AmlCaseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @PostMapping(value = "/{amlCaseId}/close", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<AmlCaseDTO>> close(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID amlCaseId,
            @RequestBody ReasonRequest request) {
        return service.close(amlCaseId, request.getStatusReason())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Remove AML case", description = "Deletes the record through core-lending-regulatory-compliance when that core allows removal.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    @DeleteMapping("/{amlCaseId}")
    public Mono<ResponseEntity<Void>> remove(
            @Parameter(description = "Core identifier", required = true) @PathVariable UUID amlCaseId) {
        return service.remove(amlCaseId)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
