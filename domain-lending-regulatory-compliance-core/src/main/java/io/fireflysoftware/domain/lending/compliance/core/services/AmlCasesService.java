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

package io.fireflysoftware.domain.lending.compliance.core.services;

import io.fireflysoftware.core.lending.compliance.sdk.model.AmlCaseDTO;
import io.fireflysoftware.domain.lending.compliance.core.commands.CreateAmlCaseCommand;
import io.fireflysoftware.domain.lending.compliance.core.commands.UpdateAmlCaseCommand;
import io.fireflysoftware.domain.lending.compliance.core.commands.UpdateAmlCaseStatusCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application service for AML case orchestration.
 */
public interface AmlCasesService {

    /**
     * Creates a AML case through the register saga.
     *
     * @param command the create command
     * @return the record returned by the core
     */
    Mono<AmlCaseDTO> create(CreateAmlCaseCommand command);

    /**
     * Moves a AML case to a new status through the core.
     *
     * @param command the status change
     * @return the updated record
     */
    Mono<AmlCaseDTO> updateStatus(UpdateAmlCaseStatusCommand command);

    /**
     * Reads one AML case.
     *
     * @param amlCaseId the core identifier
     * @return the record, or empty when the core has none
     */
    Mono<AmlCaseDTO> get(UUID amlCaseId);

    /**
     * Updates fields on a AML case and leaves its status unchanged.
     *
     * @param command the update
     * @return the record returned by the core
     */
    Mono<AmlCaseDTO> update(UpdateAmlCaseCommand command);

    /**
     * Close AML case when the core allows CLOSED.
     *
     * @param amlCaseId the core identifier
     * @param statusReason optional reason stored by the status saga when the core keeps one
     * @return the updated record
     */
    Mono<AmlCaseDTO> close(UUID amlCaseId, String statusReason);

    /**
     * Deletes a AML case when the core lifecycle allows removal.
     *
     * @param amlCaseId the core identifier
     * @return completion when the core has deleted the record
     */
    Mono<Void> remove(UUID amlCaseId);
}
