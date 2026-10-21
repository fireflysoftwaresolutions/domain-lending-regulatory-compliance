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
import io.fireflysoftware.domain.lending.compliance.core.commands.CreateAmlCaseCommand;
import io.fireflysoftware.domain.lending.compliance.core.commands.RemoveAmlCaseCommand;
import io.fireflysoftware.domain.lending.compliance.core.constants.AmlCaseSagaConstants;

import org.fireflyframework.cqrs.command.CommandBus;
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
 * Verifies the register saga stores the core result and can compensate by deleting it.
 */
@ExtendWith(MockitoExtension.class)
class RegisterAmlCaseSagaTest {

    @Mock
    private CommandBus commandBus;

    private RegisterAmlCaseSaga saga;

    @BeforeEach
    void setUp() {
        saga = new RegisterAmlCaseSaga(commandBus);
    }

    @Test
    void register_storesTheRecordReturnedByTheCore() {
        UUID id = UUID.randomUUID();
        AmlCaseDTO created = new AmlCaseDTO(id);
        created.setAmlCaseStatus(AmlCaseDTO.AmlCaseStatusEnum.OPEN);
        when(commandBus.send(any(CreateAmlCaseCommand.class))).thenReturn(Mono.just(created));

        ExecutionContext ctx = ExecutionContext.forSaga(AmlCaseSagaConstants.SAGA_REGISTER, "test");
        AmlCaseDTO request = new AmlCaseDTO();
        request.setAmlCaseStatus(AmlCaseDTO.AmlCaseStatusEnum.OPEN);

        StepVerifier.create(saga.registerAmlCase(new CreateAmlCaseCommand(request), ctx))
                .expectNext(created)
                .verifyComplete();

        assertThat(ctx.getVariableAs(AmlCaseSagaConstants.CTX_RECORD, AmlCaseDTO.class).getAmlCaseId()).isEqualTo(id);
    }

    @Test
    void remove_sendsDeleteForTheCreatedRecord() {
        UUID id = UUID.randomUUID();
        when(commandBus.send(any(RemoveAmlCaseCommand.class))).thenReturn(Mono.empty());

        StepVerifier.create(saga.removeAmlCase(new AmlCaseDTO(id)))
                .verifyComplete();

        ArgumentCaptor<RemoveAmlCaseCommand> captor = ArgumentCaptor.forClass(RemoveAmlCaseCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().amlCaseId()).isEqualTo(id);
    }

}
