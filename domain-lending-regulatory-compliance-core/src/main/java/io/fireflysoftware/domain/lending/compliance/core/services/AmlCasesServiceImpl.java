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
import io.fireflysoftware.domain.lending.compliance.core.commands.GetAmlCaseQuery;
import io.fireflysoftware.domain.lending.compliance.core.commands.RemoveAmlCaseCommand;
import io.fireflysoftware.domain.lending.compliance.core.commands.UpdateAmlCaseCommand;
import io.fireflysoftware.domain.lending.compliance.core.commands.UpdateAmlCaseStatusCommand;
import io.fireflysoftware.domain.lending.compliance.core.lifecycle.AmlCaseLifecycle;
import io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Runs the AML case sagas and reads through the query bus.
 */
@Service
public class AmlCasesServiceImpl implements AmlCasesService {

    private final SagaEngine sagaEngine;
    private final QueryBus queryBus;
    private final CommandBus commandBus;

    public AmlCasesServiceImpl(SagaEngine sagaEngine, QueryBus queryBus, CommandBus commandBus) {
        this.sagaEngine = sagaEngine;
        this.queryBus = queryBus;
        this.commandBus = commandBus;
    }

    @Override
    public Mono<AmlCaseDTO> create(CreateAmlCaseCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.STEP_REGISTER, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.SAGA_REGISTER, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.STEP_REGISTER, AmlCaseDTO.class));
    }

    @Override
    public Mono<AmlCaseDTO> updateStatus(UpdateAmlCaseStatusCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.STEP_RETRIEVE, new GetAmlCaseQuery(command.amlCaseId()))
                .forStepId(io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.STEP_UPDATE_STATUS, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.SAGA_UPDATE_STATUS, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.STEP_UPDATE_STATUS, AmlCaseDTO.class));
    }

    @Override
    public Mono<AmlCaseDTO> get(UUID amlCaseId) {
        return queryBus.query(new GetAmlCaseQuery(amlCaseId));
    }

    @Override
    public Mono<AmlCaseDTO> update(UpdateAmlCaseCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.STEP_RETRIEVE_FOR_UPDATE, new GetAmlCaseQuery(command.amlCaseId()))
                .forStepId(io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.STEP_UPDATE_RECORD, command)
                .build();
        return sagaEngine.execute(io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.SAGA_UPDATE_RECORD, inputs)
                .flatMap(result -> outcome(result, io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants.STEP_UPDATE_RECORD, AmlCaseDTO.class));
    }

    @Override
    public Mono<AmlCaseDTO> close(UUID amlCaseId, String statusReason) {
        return transition(amlCaseId, AmlCaseDTO.AmlCaseStatusEnum.CLOSED, statusReason);
    }

    private Mono<AmlCaseDTO> transition(UUID amlCaseId, AmlCaseDTO.AmlCaseStatusEnum target, String statusReason) {
        return get(amlCaseId)
                .switchIfEmpty(Mono.error(new IllegalStateException("AML case was not found")))
                .flatMap(current -> {
                    if (!AmlCaseLifecycle.canTransition(current.getAmlCaseStatus(), target)) {
                        String from = current.getAmlCaseStatus() == null
                                ? "unset"
                                : current.getAmlCaseStatus().getValue();
                        return Mono.<AmlCaseDTO>error(new IllegalStateException(
                                "AML case " + amlCaseId + " cannot move from " + from + " to " + target.getValue()));
                    }
                    return updateStatus(new UpdateAmlCaseStatusCommand(amlCaseId, target.getValue(), statusReason));
                });
    }

    @Override
    public Mono<Void> remove(UUID amlCaseId) {
        return get(amlCaseId)
                .switchIfEmpty(Mono.error(new IllegalStateException("AML case was not found")))
                .flatMap(current -> {
                    if (!AmlCaseLifecycle.canRemove(current.getAmlCaseStatus())) {
                        String statusValue = current.getAmlCaseStatus() == null
                                ? "unset"
                                : current.getAmlCaseStatus().getValue();
                        return Mono.error(new IllegalStateException(
                                "AML case " + amlCaseId + " is " + statusValue
                                        + " and cannot be deleted; close it so the case is kept"));
                    }
                    return commandBus.send(new RemoveAmlCaseCommand(amlCaseId));
                });
    }

    private static <T> Mono<T> outcome(SagaResult result, String stepId, Class<T> type) {
        if (result.isFailed()) {
            Throwable error = result.error().orElseGet(() ->
                    new IllegalStateException("Saga " + result.sagaName() + " failed"));
            return Mono.error(error);
        }
        return Mono.justOrEmpty(result.resultOf(stepId, type))
                .switchIfEmpty(Mono.error(new IllegalStateException("Saga step " + stepId + " produced no result")));
    }
}
