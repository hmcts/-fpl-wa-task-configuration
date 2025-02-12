package uk.gov.hmcts.reform.fpl.dmn;

public enum FplTask {

    REVIEW_MESSAGE_ALLOCATED_JUDGE("reviewMessageAllocatedJudge"),
    REVIEW_RESPONSE_ALLOCATED_JUDGE("reviewResponseAllocatedJudge"),
    REVIEW_MESSAGE_HEARING_JUDGE("reviewMessageHearingJudge"),
    REVIEW_RESPONSE_HEARING_JUDGE("reviewResponseHearingJudge"),
    REVIEW_MESSAGE_HEARING_CENTRE_ADMIN("reviewMessageHearingCentreAdmin"),
    REVIEW_RESPONSE_HEARING_CENTRE_ADMIN("reviewResponseHearingCentreAdmin"),
    REVIEW_MESSAGE_CTSC("reviewMessageCTSC"),
    REVIEW_RESPONSE_CTSC("reviewResponseCTSC"),
    REVIEW_MESSAGE_LEGAL_ADVISOR("reviewMessageLegalAdviser"),
    REVIEW_RESPONSE_LEGAL_ADVISOR("reviewResponseLegalAdviser"),
    VIEW_ADDITIONAL_APPLICATIONS("viewAdditionalApplications"),
    APPROVE_ORDERS("approveOrders"),
    REVIEW_URGENT_APPLICATION("reviewUrgentApplication"),
    REVIEW_STANDARD_APPLICATION("reviewStandardApplication"),
    REVIEW_STANDARD_DIRECTION_ORDER("reviewStandardDirectionOrder"),
    REVIEW_ORDER_CMO("reviewOrderCMO"),
    REVIEW_FAILED_PAYMENT("reviewFailedPayment"),
    REVIEW_CORRESPONDENCE("reviewCorrespondence"),
    CHECK_PLACEMENT_APPLICATION("checkPlacementApplication"),
    CHASE_OUTSTANDING_ORDER("chaseOutstandingOrder"),
    REVIEW_LISTING_ACTION("reviewListingAction"),
    QUERY_MANAGEMENT_RESPOND_TO_QUERY("queryManagementRespondToQuery");

    private String value;

    FplTask(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }


}
