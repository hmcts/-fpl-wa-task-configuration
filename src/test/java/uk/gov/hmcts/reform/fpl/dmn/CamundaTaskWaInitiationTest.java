package uk.gov.hmcts.reform.fpl.dmn;

import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.impl.VariableMapImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.fpl.DmnDecisionTable;
import uk.gov.hmcts.reform.fpl.DmnDecisionTableBaseUnitTest;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.fpl.dmn.ProcessCategory.CASE_CREATION;
import static uk.gov.hmcts.reform.fpl.dmn.ProcessCategory.CASE_PROGRESSION;
import static uk.gov.hmcts.reform.fpl.dmn.ProcessCategory.MANAGE_OUTCOME;

class CamundaTaskWaInitiationTest extends DmnDecisionTableBaseUnitTest {

    @BeforeAll
    public static void initialization() {
        CURRENT_DMN_DECISION_TABLE = DmnDecisionTable.WA_TASK_INITIATION;
    }

    @ParameterizedTest
    @MethodSource("scenarioProvider")
    void givenInputShouldReturnOutcomeDmn(String eventId,
                                               Map<String, String> additionalData,
                                               Map<String, ? extends Serializable> expectedDmnOutcome) {
        VariableMap inputVariables = new VariableMapImpl();
        inputVariables.putValue("eventId", eventId);
        inputVariables.putValue("additionalData", Map.of("Data", additionalData));

        DmnDecisionTableResult dmnDecisionTableResult = evaluateDmnTable(inputVariables);

        assertThat(dmnDecisionTableResult.getResultList(), is(singletonList(expectedDmnOutcome)));
    }

    public static Stream<Arguments> scenarioProvider() {
        return Stream.of(
            Arguments.of(
                "messageJudgeOrLegalAdviser",
                Map.of(
                    "latestRoleSent", "JUDICIARY"
                ),
                Map.of(
                    "taskId", "reviewMessageAllocatedJudge",
                    "name", "Review Message (Allocated Judge)",
                    "processCategories", CASE_PROGRESSION.getValue()
                )
            ),
            Arguments.of(
                "replyToMessageJudgeOrLegalAdviser",
                Map.of(
                    "latestRoleSent", "JUDICIARY"
                ),
                Map.of(
                    "taskId", "reviewResponseAllocatedJudge",
                    "name", "Review Response (Allocated Judge)",
                    "processCategories", CASE_PROGRESSION.getValue()
                )
            ),
            Arguments.of(
                "messageJudgeOrLegalAdviser",
                Map.of(
                    "latestRoleSent", "HEARING_JUDGE"
                ),
                Map.of(
                    "taskId", "reviewMessageHearingJudge",
                    "name", "Review Message (Hearing Judge)",
                    "processCategories", CASE_PROGRESSION.getValue()
                )
            ),
            Arguments.of(
                "messageJudgeOrLegalAdviser",
                Map.of(
                    "latestRoleSent", "LOCAL_COURT_ADMIN"
                ),
                Map.of(
                    "taskId", "reviewMessageHearingCentreAdmin",
                    "name", "Review Message (Local Court)",
                    "processCategories", CASE_PROGRESSION.getValue()
                )
            ),
            Arguments.of(
                "messageJudgeOrLegalAdviser",
                Map.of(
                    "latestRoleSent", "CTSC"
                ),
                Map.of(
                    "taskId", "reviewMessageCTSC",
                    "name", "Review Message (CTSC)",
                    "processCategories", CASE_PROGRESSION.getValue()
                )
            ),
            Arguments.of(
                "messageJudgeOrLegalAdviser",
                Map.of(
                    "latestRoleSent", "OTHER"
                ),
                Map.of(
                    "taskId", "reviewMessageLegalAdviser",
                    "name", "Review Message (Legal Adviser)",
                    "processCategories", CASE_PROGRESSION.getValue()
                )
            ),
            Arguments.of(
                "submitApplication",
                Map.of(
                    "hearing", Map.of("timeFrame", "Within 2 days")
                ),
                Map.of(
                    "taskId", "reviewUrgentApplication",
                    "name", "Review Urgent Application",
                    "processCategories", CASE_CREATION.getValue()
                )
            ),
            Arguments.of(
                "submitApplication",
                Map.of(
                    "hearing", Map.of("timeFrame", "Within 7 days")
                ),
                Map.of(
                    "taskId", "reviewStandardApplication",
                    "name", "Review Standard Application",
                    "processCategories", CASE_CREATION.getValue()
                )
            ),
            Arguments.of(
                "create-work-allocation-task",
                Map.of(
                    "lastCreatedWATask", "ORDER_UPLOADED"
                ),
                Map.of(
                    "taskId", "reviewOrderCMO",
                    "name", "Review Order",
                    "processCategories", MANAGE_OUTCOME.getValue()
                )
            )
        );
    }

    @Test
    void shouldHaveCorrectNumberOfRules() {
        // The purpose of this test is to prevent adding new rows without being tested
        DmnDecisionTableImpl logic = (DmnDecisionTableImpl) decision.getDecisionLogic();
        // todo - check this after evaluation period
        assertThat(logic.getRules().size(), is(21));
    }
}
