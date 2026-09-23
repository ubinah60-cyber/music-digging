package com.example.musicdigging.digging.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrackCreditTest {

    @Test
    void sameIdAndRoleMatchEvenWhenNamesDiffer() {
        TrackCredit first = new TrackCredit(
                "person-001", "홍길동", CreditRole.COMPOSER
        );
        TrackCredit second = new TrackCredit(
                "person-001", "Hong Gil Dong", CreditRole.COMPOSER
        );

        assertTrue(first.hasSamePersonAndRole(second));
    }

    @Test
    void sameNameWithDifferentIdsDoesNotMatch() {
        TrackCredit first = new TrackCredit(
                "person-001", "홍길동", CreditRole.COMPOSER
        );
        TrackCredit second = new TrackCredit(
                "person-002", "홍길동", CreditRole.COMPOSER
        );

        assertFalse(first.hasSamePersonAndRole(second));
    }

    @Test
    void samePersonWithDifferentRolesDoesNotMatch() {
        TrackCredit first = new TrackCredit(
                "person-001", "홍길동", CreditRole.COMPOSER
        );
        TrackCredit second = new TrackCredit(
                "person-001", "홍길동", CreditRole.PRODUCER
        );

        assertFalse(first.hasSamePersonAndRole(second));
    }

    @Test
    void blankPersonIdIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TrackCredit(
                        " ", "홍길동", CreditRole.COMPOSER
                )
        );
    }
}