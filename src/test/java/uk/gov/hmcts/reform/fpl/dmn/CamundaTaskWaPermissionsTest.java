package uk.gov.hmcts.reform.fpl.dmn;

import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.impl.VariableMapImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import uk.gov.hmcts.reform.fpl.DmnDecisionTable;
import uk.gov.hmcts.reform.fpl.DmnDecisionTableBaseUnitTest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamundaTaskWaPermissionsTest extends DmnDecisionTableBaseUnitTest {

    @BeforeAll
    public static void initialization() {
        CURRENT_DMN_DECISION_TABLE = DmnDecisionTable.WA_TASK_PERMISSIONS;
    }

    // @ParameterizedTest
    // @MethodSource("scenarioProvider")
    void givenInputShouldReturnOutcomeDmn(FplTask taskType,
                                          List<Map<String, ? extends Serializable>> expectedDmnOutcome) {
        VariableMap inputVariables = new VariableMapImpl();
        inputVariables.putValue("taskType", taskType.getValue());

        DmnDecisionTableResult dmnDecisionTableResult = evaluateDmnTable(inputVariables);

        assertTrue(dmnDecisionTableResult.getResultList().containsAll(expectedDmnOutcome));
    }

    public static Stream<Arguments> scenarioProvider() {
        return Stream.of(
            Arguments.of(FplTask.VIEW_ADDITIONAL_APPLICATIONS, List.of(
                getRowResult(
                    "task-supervisor",
                    "Complete,Own,Assign,Claim,Unassign,Read,Cancel",
                    null,
                    null,
                    null,
                    false
                ),
                getRowResult(
                    "allocated-judge",
                    "Complete,Own,Assign,Claim,Unassign,Read",
                    "JUDICIAL",
                    "316",
                    1,
                    true
                ),
                getRowResult(
                    "tribunal-caseworker",
                    "Complete,Own,Assign,Claim,Unassign,Read",
                    "LEGAL_OPERATIONS",
                    null,
                    null,
                    false
                )
            ))
        );
    }

    @Test
    void shouldHaveCorrectNumberOfRules() {
        // The purpose of this test is to prevent adding new rows without being tested
        DmnDecisionTableImpl logic = (DmnDecisionTableImpl) decision.getDecisionLogic();
        assertThat(logic.getRules().size(), is(27));
    }

    private static Map<String, Object> getRowResult(String name, String value, String roleCategory, String auth,
                                                    Integer assignmentPriority, boolean autoAssignable) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", name);
        row.put("value", value);
        row.put("autoAssignable", autoAssignable);
        if (roleCategory != null) {
            row.put("roleCategory", roleCategory);
        }
        if (auth != null) {
            row.put("authorisations", auth);
        }
        if (assignmentPriority != null) {
            row.put("assignmentPriority", assignmentPriority);
        }
        return row;
    }

}
