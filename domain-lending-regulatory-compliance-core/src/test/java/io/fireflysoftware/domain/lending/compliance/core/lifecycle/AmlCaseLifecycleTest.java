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

package io.fireflysoftware.domain.lending.compliance.core.lifecycle;

import io.fireflysoftware.core.lending.compliance.sdk.model.AmlCaseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the domain copy of the core lifecycle.
 */
class AmlCaseLifecycleTest {

    @Test
    void allowsTheCoreMove() {
        assertThat(AmlCaseLifecycle.canTransition(AmlCaseDTO.AmlCaseStatusEnum.OPEN, AmlCaseDTO.AmlCaseStatusEnum.INVESTIGATING)).isTrue();
    }

    @Test
    void refusesAMissingTarget() {
        assertThat(AmlCaseLifecycle.canTransition(AmlCaseDTO.AmlCaseStatusEnum.OPEN, null)).isFalse();
    }

    @Test
    void removeFollowsTheCoreRule() {

        assertThat(AmlCaseLifecycle.canRemove(null)).isTrue();
        assertThat(AmlCaseLifecycle.canRemove(AmlCaseDTO.AmlCaseStatusEnum.OPEN)).isTrue();
        assertThat(AmlCaseLifecycle.canRemove(AmlCaseDTO.AmlCaseStatusEnum.INVESTIGATING)).isTrue();
    }
}
