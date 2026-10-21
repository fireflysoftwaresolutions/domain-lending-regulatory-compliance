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

package io.fireflysoftware.domain.lending.compliance.core.constants;

/**
 * Names shared by the AML case sagas, their steps, and the saga engine.
 */
public final class AmlCaseSagaConstants {

    public static final String SAGA_REGISTER = "RegisterAmlCaseSaga";
    public static final String SAGA_UPDATE_STATUS = "UpdateAmlCaseStatusSaga";
    public static final String SAGA_UPDATE_RECORD = "UpdateAmlCaseSaga";

    public static final String STEP_REGISTER = "registerAmlCase";
    public static final String STEP_RETRIEVE = "retrieveAmlCase";
    public static final String STEP_UPDATE_STATUS = "updateAmlCaseStatus";
    public static final String STEP_RETRIEVE_FOR_UPDATE = "retrieveAmlCaseForUpdate";
    public static final String STEP_UPDATE_RECORD = "updateAmlCase";

    public static final String COMPENSATE_REMOVE = "removeAmlCase";
    public static final String COMPENSATE_RESTORE = "restoreAmlCaseStatus";
    public static final String COMPENSATE_RESTORE_RECORD = "restoreAmlCase";

    public static final String EVENT_REGISTERED = "aml-cases.registered";
    public static final String EVENT_RETRIEVED = "aml-cases.retrieved";
    public static final String EVENT_STATUS_UPDATED = "aml-cases.status.updated";
    public static final String EVENT_RECORD_UPDATED = "aml-cases.updated";

    public static final String CTX_RECORD = "aml-casesRecord";
    public static final String CTX_PREVIOUS_STATUS = "previousAmlCaseStatus";
    public static final String CTX_PREVIOUS_RECORD = "previousAmlCaseRecord";

    private AmlCaseSagaConstants() {
    }
}
