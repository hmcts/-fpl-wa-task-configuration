package uk.gov.hmcts.reform.fpl.dmn;

import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.impl.VariableMapImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fpl.DmnDecisionTable;
import uk.gov.hmcts.reform.fpl.DmnDecisionTableBaseUnitTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamundaTaskWaConfigurationTest extends DmnDecisionTableBaseUnitTest {

    @BeforeAll
    public static void initialization() {
        CURRENT_DMN_DECISION_TABLE = DmnDecisionTable.WA_TASK_CONFIGURATION;
    }

    private static LocalDateTime NOW = LocalDateTime.now();

    private static Map<String, Object> CASE_DATA = Map.of(
        "caseName", "Test v Smith",
        "caseManagementLocation", Map.of(
            "region", "1",
            "baseLocation", "111"
        ),
        "court", Map.of("name", "Birmingham"),
        "caseSummaryNextHearingDate", NOW
    );

    @Test
    void shouldHaveBasicRules() {
        VariableMap inputVariables = new VariableMapImpl();
        inputVariables.putValue("taskType", "");
        inputVariables.putValue("caseData", CASE_DATA);

        DmnDecisionTableResult dmnDecisionTableResult = evaluateDmnTable(inputVariables);
        assertTrue(dmnDecisionTableResult.getResultList().containsAll(getBaseValues()));
    }

    @Test
    void shouldHaveCorrectPriorities() {
        VariableMap inputVariables = new VariableMapImpl();
        inputVariables.putValue("taskType", "checkPlacementApplication");
        inputVariables.putValue("caseData", CASE_DATA);

        DmnDecisionTableResult dmnDecisionTableResult = evaluateDmnTable(inputVariables);
        assertTrue(dmnDecisionTableResult.getResultList().containsAll(List.of(
            getRowResult("minorPriority", "500", true),
            getRowResult("majorPriority", "5000", true)
        )));
    }

    @Test
    void shouldHaveCorrectNumberOfRules() {
        // The purpose of this test is to prevent adding new rows without being tested
        DmnDecisionTableImpl logic = (DmnDecisionTableImpl) decision.getDecisionLogic();
        assertThat(logic.getRules().size(), is(65));
    }

    private static List<Map<String, Object>> getBaseValues() {
        return List.of(
            getRowResult("caseName", "Test v Smith", true),
            getRowResult("caseManagementCategory", "PUBLICLAW", false),
            getRowResult("region", "1", true),
            getRowResult("location", "111", true),
            getRowResult("locationName", "Birmingham", true)
        );
    }

    private static Map<String, Object> getRowResult(String name, String value, boolean canReconfigure) {
        return Map.of(
            "canReconfigure", canReconfigure,
            "name", name,
            "value", value
        );
    }
}
