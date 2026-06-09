package com.app.digitalwallet.domain.model

enum class KycStatus(val value: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    NOT_SUBMITTED("not_submitted"),
    UNKNOWN("unknown");

    companion object {
        fun from(value: String?): KycStatus =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
