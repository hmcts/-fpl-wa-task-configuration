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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class CamundaTaskWaCompletionTest extends DmnDecisionTableBaseUnitTest {

    @BeforeAll
    public static void initialization() {
        CURRENT_DMN_DECISION_TABLE = DmnDecisionTable.WA_TASK_COMPLETION;
    }

    @ParameterizedTest
    @MethodSource("scenarioProvider")
    void givenInputShouldReturnOutcomeDmn(String eventId,
                                          List<Map<String, ? extends Serializable>> expectedDmnOutcome) {
        VariableMap inputVariables = new VariableMapImpl();
        inputVariables.putValue("eventId", eventId);

        DmnDecisionTableResult dmnDecisionTableResult = evaluateDmnTable(inputVariables);

        assertThat(dmnDecisionTableResult.getResultList(), is(expectedDmnOutcome));
    }

    public static Stream<Arguments> scenarioProvider() {
        return Stream.of(
            Arguments.of("reviewCMO", getAutoCompleteTaskTypes(FplTask.APPROVE_ORDERS)),
            Arguments.of("replyToMessageJudgeOrLegalAdviser", getAutoCompleteTaskTypes(
                FplTask.REVIEW_MESSAGE_ALLOCATED_JUDGE, FplTask.REVIEW_RESPONSE_ALLOCATED_JUDGE,
                FplTask.REVIEW_MESSAGE_HEARING_JUDGE, FplTask.REVIEW_RESPONSE_HEARING_JUDGE,
                FplTask.REVIEW_MESSAGE_HEARING_CENTRE_ADMIN, FplTask.REVIEW_RESPONSE_HEARING_CENTRE_ADMIN,
                FplTask.REVIEW_MESSAGE_CTSC, FplTask.REVIEW_RESPONSE_CTSC, FplTask.REVIEW_MESSAGE_LEGAL_ADVISOR,
                FplTask.REVIEW_RESPONSE_LEGAL_ADVISOR
            )),
            Arguments.of("sendToGatekeeper", getAutoCompleteTaskTypes(
                FplTask.REVIEW_URGENT_APPLICATION,
                FplTask.REVIEW_STANDARD_APPLICATION
            )),
            Arguments.of("returnApplication", getAutoCompleteTaskTypes(
                FplTask.REVIEW_URGENT_APPLICATION,
                FplTask.REVIEW_STANDARD_APPLICATION
            )),
            Arguments.of("reviewListingAction", getAutoCompleteTaskTypes(FplTask.REVIEW_LISTING_ACTION))
        );
    }

    @Test
    void shouldHaveCorrectNumberOfRules() {
        // The purpose of this test is to prevent adding new rows without being tested
        DmnDecisionTableImpl logic = (DmnDecisionTableImpl) decision.getDecisionLogic();
        assertThat(logic.getRules().size(), is(19));
    }

    private static Map<String, String> getAutoCompleteTaskType(FplTask taskType) {
        return Map.of(
            "taskType", taskType.getValue(),
            "completionMode", "Auto"
        );
    }

    private static List<Map<String, String>> getAutoCompleteTaskTypes(FplTask... taskTypes) {
        List<Map<String, String>> tasks = Stream.of(taskTypes)
            .map(el -> getAutoCompleteTaskType(el))
            .collect(Collectors.toList());
        tasks.add(Map.of()); // add non-completion events
        return tasks;
    }
}
