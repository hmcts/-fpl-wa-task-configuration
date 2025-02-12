package uk.gov.hmcts.reform.fpl.dmn;

import camundajar.impl.scala.math.BigDecimal;
import camundajar.impl.scala.util.Either;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.impl.VariableMapImpl;
import org.camunda.feel.FeelEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.fpl.DmnDecisionTable;
import uk.gov.hmcts.reform.fpl.DmnDecisionTableBaseUnitTest;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.ObjectUtils.isEmpty;

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
    void shouldSetTitleOnListingAction() {
        VariableMap inputVariables = new VariableMapImpl();
        inputVariables.putValue("taskType", "reviewListingAction");
        inputVariables.putValue("taskAttributes", Map.of("name", "Review listing request",
                                                         "__processCategory__actionType_Listing required", true));
        inputVariables.putValue("caseData", CASE_DATA);

        DmnDecisionTableResult dmnDecisionTableResult = evaluateDmnTable(inputVariables);
        assertTrue(dmnDecisionTableResult.getResultList()
                       .contains(getRowResult("title", "Review listing request (Listing required)", false)));
    }

    @Test
    void shouldHaveCorrectPriorities() {
        VariableMap inputVariables = new VariableMapImpl();
        inputVariables.putValue("taskType", "checkPlacementApplication");
        inputVariables.putValue("caseData", CASE_DATA);

        DmnDecisionTableResult dmnDecisionTableResult = evaluateDmnTable(inputVariables);
        assertTrue(dmnDecisionTableResult.getResultList().containsAll(List.of(
            getRowResult("minorPriority", "500", true),
            getRowResult("majorPriority", "5000", false)
        )));
    }

    private static Map<String, Object> toNullValueMap(String key) {
        Map<String, Object> ret = new HashMap<>();
        ret.put(key, null);
        return ret;
    }

    private static Stream<Arguments> approveOrdersMajorPriorityScenarios() {
        return Stream.of(
            Arguments.of(2000, Map.of("draftOrderUrgency", Map.of("urgency", List.of("YES")))),
            Arguments.of(5000, Map.of("draftOrderUrgency", Map.of("urgency", List.of("NO")))),
            Arguments.of(5000, Map.of("draftOrderUrgency", toNullValueMap("urgency"))),
            Arguments.of(5000, toNullValueMap("draftOrderUrgency")),
            Arguments.of(5000, null)
        );
    }

    @ParameterizedTest
    @MethodSource("approveOrdersMajorPriorityScenarios")
    void testApproveOrdersMajorPriority(int expected, Map<String, Object> caseData) {
        DmnDecisionTableImpl logic = (DmnDecisionTableImpl) decision.getDecisionLogic();
        String feelExpression = getValueFromWaConfiguration(logic, "approveOrders", "majorPriority");

        FeelEngine feelEngine = new FeelEngine.Builder().build();

        Either<FeelEngine.Failure, Object> result =
            feelEngine.evalExpression(feelExpression, caseData == null ? toNullValueMap("caseData")
                : Map.of("caseData", caseData));
        assertEquals(BigDecimal.decimal(expected), result.toOption().get());
    }

    private static String encloseDoubleQuote(String name) {
        return "\"" + name + "\"";
    }

    private static String getValueFromWaConfiguration(DmnDecisionTableImpl logic, String taskType, String name) {
        return logic.getRules().stream()
            .filter(d -> !isEmpty(d.getConditions().get(1).getExpression())
                && d.getConditions().get(1).getExpression().contains(encloseDoubleQuote(taskType))
                && encloseDoubleQuote(name).equals(d.getConclusions().get(0).getExpression()))
            .findAny()
            .orElseThrow(() -> new NoSuchElementException("Unable to locate " + taskType + " and " + name
                                                              + " from the DMN table"))
            .getConclusions().get(1).getExpression();
    }

    private static Stream<Arguments> allocatedJudgeScenarios() {
        return Stream.of(
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("allocatedJudge", Map.of("judgeTitle", "LEGAL_ADVISOR"))
            ),
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("allocatedJudge", Map.of("judgeTitle", "HER_HONOUR_JUDGE",
                                                "judgeEmailAddress", "test@justice.gov.uk"
                ))
            ),
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("allocatedJudge", Map.of("judgeTitle", "MAGISTRATE",
                                                "judgeEmailAddress", "test@Justice.gov.uk"
                ))
            ),
            Arguments.of(
                "JUDICIAL",
                Map.of("allocatedJudge", Map.of("judgeTitle", "HER_HONOUR_JUDGE",
                                                "judgeEmailAddress", "test@whatever.com"
                ))
            ),
            Arguments.of(
                "JUDICIAL",
                Map.of("allocatedJudge", Map.of())
            )
        );
    }

    @ParameterizedTest
    @MethodSource("allocatedJudgeScenarios")
    void testViewAdditionalApplicationRoleCategory(String expected, Map<String, Object> caseData) {
        DmnDecisionTableImpl logic = (DmnDecisionTableImpl) decision.getDecisionLogic();
        String feelExpression = getValueFromWaConfiguration(logic, "viewAdditionalApplications", "roleCategory");

        FeelEngine feelEngine = new FeelEngine.Builder().build();

        Either<FeelEngine.Failure, Object> result =
            feelEngine.evalExpression(feelExpression, Map.of("caseData", caseData));
        assertEquals(expected, result.toOption().get());
    }

    @ParameterizedTest
    @MethodSource("allocatedJudgeScenarios")
    void testMessageResponseAllocatedJudgeRoleCategory(String expected, Map<String, Object> caseData) {
        DmnDecisionTableImpl logic = (DmnDecisionTableImpl) decision.getDecisionLogic();
        String feelExpression = getValueFromWaConfiguration(logic, "reviewMessageAllocatedJudge", "roleCategory");

        FeelEngine feelEngine = new FeelEngine.Builder().build();

        Either<FeelEngine.Failure, Object> result =
            feelEngine.evalExpression(feelExpression, Map.of("caseData", caseData));
        assertEquals(expected, result.toOption().get());
    }

    @ParameterizedTest
    @MethodSource("hearingJudgeScenarios")
    void testMessageResponseHearingJudgeRoleCategory(String expected, Map<String, Object> caseData) {
        DmnDecisionTableImpl logic = (DmnDecisionTableImpl) decision.getDecisionLogic();
        String feelExpression = getValueFromWaConfiguration(logic, "reviewMessageHearingJudge", "roleCategory");

        FeelEngine feelEngine = new FeelEngine.Builder().build();

        Either<FeelEngine.Failure, Object> result =
            feelEngine.evalExpression(feelExpression, Map.of("caseData", caseData));
        assertEquals(expected, result.toOption().get());
    }


    private static String formatString(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date) + "T"
            + new SimpleDateFormat("HH:mm:ss").format(date);
    }

    private static String getFutureStartDate() {
        return getStartDate(1);
    }

    private static String getPastStartDate() {
        return getStartDate(-1);
    }

    private static String getStartDate(int delta) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, delta);
        return formatString(cal.getTime());
    }

    private static Stream<Arguments> hearingJudgeScenarios() {
        return Stream.of(
            // empty hearingDetails, determined by allocatedJudge.judgeTitle
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(),
                       "allocatedJudge", Map.of("judgeTitle", "LEGAL_ADVISOR")
                )
            ),
            Arguments.of(
                "JUDICIAL",
                Map.of("hearingDetails", List.of(),
                       "allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE")
                )
            ),
            // empty hearingDetails, determined by allocatedJudge.judgeEmailAddress
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(),
                       "allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@justice.gov.uk"
                    )
                )
            ),
            Arguments.of(
                "JUDICIAL",
                Map.of("hearingDetails", List.of(),
                       "allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@whatever.com"
                    )
                )
            ),
            // null hearingDetails, determined by allocatedJudge.judgeTitle
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("allocatedJudge", Map.of("judgeTitle", "LEGAL_ADVISOR")
                )
            ),
            Arguments.of(
                "JUDICIAL",
                Map.of("allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE")
                )
            ),
            // null hearingDetails, determined by allocatedJudge.judgeEmailAddress
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@justice.gov.uk"
                       )
                )
            ),
            Arguments.of(
                "JUDICIAL",
                Map.of("allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@whatever.com"
                       )
                )
            ),
            // future hearing only, determined by allocatedJudge.judgeTitle
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getFutureStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "LEGAL_ADVISOR")
                )
            ),
            // future hearing only, determined by allocatedJudge.judgeEmailAddress
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getFutureStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@justice.gov.uk"
                    )
                )
            ),
            // Determined by active hearing (only one hearing)
            Arguments.of(
                "JUDICIAL",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getPastStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "LEGAL_ADVISOR",
                                                "judgeEmailAddress", "whatever@justice.gov.uk"
                    )
                )
            ),
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getPastStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@justice.gov.uk"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@whatever.com"
                    )
                )
            ),
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getPastStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "LEGAL_ADVISOR",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@whatever.com"
                    )
                )
            ),
            // Determined by active hearing (1st hearing is active)
            Arguments.of(
                "JUDICIAL",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getPastStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           ),
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getFutureStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "LEGAL_ADVISOR",
                                       "judgeEmailAddress", "whatever@justice.gov.uk"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "LEGAL_ADVISOR",
                                                "judgeEmailAddress", "whatever@justice.gov.uk"
                    )
                )
            ),
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getPastStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MAGISTRATE",
                                       "judgeEmailAddress", "whatever@Justice.gov.uk"
                                   )
                               )
                           ),
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getFutureStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "LEGAL_ADVISOR",
                                       "judgeEmailAddress", "whatever@justice.gov.uk"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "LEGAL_ADVISOR",
                                                "judgeEmailAddress", "whatever@justice.gov.uk"
                    )
                )
            ),
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getPastStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "LEGAL_ADVISOR",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           ),
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getFutureStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@whatever.com"
                    )
                )
            ),
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getPastStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@justice.gov.uk"
                                   )
                               )
                           ),
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getFutureStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@whatever.com"
                    )
                )
            ),
            // Determined by active hearing (2nd hearing is active)
            Arguments.of(
                "JUDICIAL",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getStartDate(-2),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "LEGAL_ADVISOR",
                                       "judgeEmailAddress", "whatever@justice.gov.uk"
                                   )
                               )
                           ),
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getStartDate(-1),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           ),
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getFutureStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "LEGAL_ADVISOR",
                                       "judgeEmailAddress", "whatever@justice.gov.uk"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "LEGAL_ADVISOR",
                                                "judgeEmailAddress", "whatever@justice.gov.uk"
                    )
                )
            ),
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getStartDate(-2),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           ),
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getStartDate(-1),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "LEGAL_ADVISOR",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           ),
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getFutureStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@whatever.com"
                    )
                )
            ),
            Arguments.of(
                "LEGAL_OPERATIONS",
                Map.of("hearingDetails", List.of(
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getStartDate(-2),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           ),
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getStartDate(-1),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@justice.gov.uk"
                                   )
                               )
                           ),
                           Map.of("id", UUID.randomUUID(),
                                  "value", Map.of(
                                   "startDate", getFutureStartDate(),
                                   "judgeAndLegalAdvisor", Map.of(
                                       "judgeTitle", "MR_JUSTICE",
                                       "judgeEmailAddress", "whatever@whatever.com"
                                   )
                               )
                           )
                       ),
                       "allocatedJudge", Map.of("judgeTitle", "MR_JUSTICE",
                                                "judgeEmailAddress", "whatever@whatever.com"
                    )
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource("hearingJudgeScenarios")
    void testApproveOrdersRoleCategory(String expected, Map<String, Object> caseData) {
        DmnDecisionTableImpl logic = (DmnDecisionTableImpl) decision.getDecisionLogic();
        String feelExpression = getValueFromWaConfiguration(logic, "approveOrders", "roleCategory");

        FeelEngine feelEngine = new FeelEngine.Builder().build();

        Either<FeelEngine.Failure, Object> result =
            feelEngine.evalExpression(feelExpression, Map.of("caseData", caseData));
        assertEquals(expected, result.toOption().get());
    }

    @Test
    void shouldHaveCorrectNumberOfRules() {
        // The purpose of this test is to prevent adding new rows without being tested
        DmnDecisionTableImpl logic = (DmnDecisionTableImpl) decision.getDecisionLogic();
        assertThat(logic.getRules().size(), is(70));
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
