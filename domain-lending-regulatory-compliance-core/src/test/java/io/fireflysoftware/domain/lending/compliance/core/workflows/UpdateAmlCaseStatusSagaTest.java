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

package io.fireflysoftware.domain.lending.compliance.core.workflows;

import io.fireflysoftware.core.lending.compliance.sdk.model.AmlCaseDTO;
import io.fireflysoftware.domain.lending.compliance.core.commands.ApplyAmlCaseStatusCommand;
import io.fireflysoftware.domain.lending.compliance.core.commands.UpdateAmlCaseStatusCommand;
import io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants;

import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies a status change is written through the core and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateAmlCaseStatusSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateAmlCaseStatusSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateAmlCaseStatusSaga(commandBus, queryBus);
    }

    @Test
    void updateStatus_sendsTheRequestedStatusToTheCore() {
        UUID id = UUID.randomUUID();
        AmlCaseDTO current = new AmlCaseDTO(id);
        current.setAmlCaseStatus(AmlCaseDTO.AmlCaseStatusEnum.OPEN);
        when(commandBus.send(any(ApplyAmlCaseStatusCommand.class))).thenReturn(Mono.just(current));

        ExecutionContext ctx = ExecutionContext.forSaga(AmlCaseSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(AmlCaseSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateAmlCaseStatus(new UpdateAmlCaseStatusCommand(id, "INVESTIGATING", "Caller request"), ctx))
                .expectNext(current)
                .verifyComplete();

        ArgumentCaptor<ApplyAmlCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyAmlCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().amlCaseId()).isEqualTo(id);
        assertThat(captor.getValue().record().getAmlCaseStatus()).isEqualTo(AmlCaseDTO.AmlCaseStatusEnum.INVESTIGATING);
        assertThat(ctx.getVariableAs(AmlCaseSagaConstants.CTX_PREVIOUS_STATUS, String.class)).isEqualTo("OPEN");
    }

    @Test
    void restore_writesThePreviousStatusBack() {
        UUID id = UUID.randomUUID();
        AmlCaseDTO updated = new AmlCaseDTO(id);
        updated.setAmlCaseStatus(AmlCaseDTO.AmlCaseStatusEnum.INVESTIGATING);
        when(commandBus.send(any(ApplyAmlCaseStatusCommand.class))).thenReturn(Mono.just(updated));

        ExecutionContext ctx = ExecutionContext.forSaga(AmlCaseSagaConstants.SAGA_UPDATE_STATUS, "test");
        ctx.putVariable(AmlCaseSagaConstants.CTX_PREVIOUS_STATUS, "OPEN");

        StepVerifier.create(saga.restoreAmlCaseStatus(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyAmlCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyAmlCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record().getAmlCaseStatus()).isEqualTo(AmlCaseDTO.AmlCaseStatusEnum.OPEN);
    }

}
