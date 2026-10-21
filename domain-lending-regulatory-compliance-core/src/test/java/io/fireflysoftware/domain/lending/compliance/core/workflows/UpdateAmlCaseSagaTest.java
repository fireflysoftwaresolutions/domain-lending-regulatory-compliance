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
import io.fireflysoftware.domain.lending.compliance.core.commands.UpdateAmlCaseCommand;
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
 * Verifies a field update keeps the current status and can be restored.
 */
@ExtendWith(MockitoExtension.class)
class UpdateAmlCaseSagaTest {

    @Mock
    private CommandBus commandBus;

    @Mock
    private QueryBus queryBus;

    private UpdateAmlCaseSaga saga;

    @BeforeEach
    void setUp() {
        saga = new UpdateAmlCaseSaga(commandBus, queryBus);
    }

    @Test
    void update_keepsTheCurrentStatus() {
        UUID id = UUID.randomUUID();
        AmlCaseDTO current = new AmlCaseDTO(id);
        current.setAmlCaseStatus(AmlCaseDTO.AmlCaseStatusEnum.OPEN);
        AmlCaseDTO incoming = new AmlCaseDTO();
        incoming.setAmlCaseStatus(AmlCaseDTO.AmlCaseStatusEnum.INVESTIGATING);
        when(commandBus.send(any(ApplyAmlCaseStatusCommand.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0, ApplyAmlCaseStatusCommand.class).record()));

        ExecutionContext ctx = ExecutionContext.forSaga(AmlCaseSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(AmlCaseSagaConstants.CTX_RECORD, current);

        StepVerifier.create(saga.updateAmlCase(new UpdateAmlCaseCommand(id, incoming), ctx))
                .assertNext(saved -> assertThat(saved.getAmlCaseStatus()).isEqualTo(AmlCaseDTO.AmlCaseStatusEnum.OPEN))
                .verifyComplete();

        ArgumentCaptor<ApplyAmlCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyAmlCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().amlCaseId()).isEqualTo(id);
        assertThat(captor.getValue().record().getAmlCaseStatus()).isEqualTo(AmlCaseDTO.AmlCaseStatusEnum.OPEN);
        assertThat(ctx.getVariableAs(AmlCaseSagaConstants.CTX_PREVIOUS_RECORD, AmlCaseDTO.class)).isSameAs(current);
    }

    @Test
    void restore_writesThePreviousRecordBack() {
        UUID id = UUID.randomUUID();
        AmlCaseDTO previous = new AmlCaseDTO(id);
        previous.setAmlCaseStatus(AmlCaseDTO.AmlCaseStatusEnum.OPEN);
        AmlCaseDTO updated = new AmlCaseDTO(id);
        updated.setAmlCaseStatus(AmlCaseDTO.AmlCaseStatusEnum.INVESTIGATING);
        when(commandBus.send(any(ApplyAmlCaseStatusCommand.class))).thenReturn(Mono.just(previous));

        ExecutionContext ctx = ExecutionContext.forSaga(AmlCaseSagaConstants.SAGA_UPDATE_RECORD, "test");
        ctx.putVariable(AmlCaseSagaConstants.CTX_PREVIOUS_RECORD, previous);

        StepVerifier.create(saga.restoreAmlCase(updated, ctx))
                .verifyComplete();

        ArgumentCaptor<ApplyAmlCaseStatusCommand> captor = ArgumentCaptor.forClass(ApplyAmlCaseStatusCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().record()).isSameAs(previous);
    }
}
