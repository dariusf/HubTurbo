package tests;

import org.junit.Test;
import util.RefreshTimer;

import static org.junit.Assert.*;

/**
 * Tests the correct behaviour of the methods in the RefreshTimer class.
 */
public class RefreshTimerTest {

    @Test
    public void computeTickerTimer_initialAppLaunch() {
        long result = RefreshTimer.computeRefreshTimerPeriod(100, 10, 0, 1, 1);
        assertEquals(result, 1);
    }

    @Test
    public void computeTickerTimer_outOfQuota() {
        long result = RefreshTimer.computeRefreshTimerPeriod(0, 35, 0, 0, 1);
        assertEquals(result, 36);
    }

    @Test
    public void computeTickerTimer_minimalQuotaWithApiQuotaEqualsApiQuotaBuffer() {
        long result = RefreshTimer.computeRefreshTimerPeriod(1, 35, 15, 1, 1);
        assertEquals(result, 36);
    }

    @Test
    public void computeTickerTimer_minimalQuotaWithApiQuotaBufferZero() {
        long result = RefreshTimer.computeRefreshTimerPeriod(1, 35, 15, 0, 1);
        assertEquals(result, 36);
    }

    @Test
    public void computeTickerTimer_quotaBelowApiQuotaBuffer() {
        long result = RefreshTimer.computeRefreshTimerPeriod(199, 35, 25, 200, 1);
        assertEquals(result, 36);

        result = RefreshTimer.computeRefreshTimerPeriod(1, 35, 25, 2, 60);
        assertEquals(result, 60);
    }

    @Test
    public void computeTickerTimer_quotaAtApiQuotaBuffer() {
        long result = RefreshTimer.computeRefreshTimerPeriod(200, 35, 25, 200, 1);
        assertEquals(result, 36);
    }

    @Test
    public void computeTickerTimer_quotaAboveApiQuotaBuffer() {
        long result;
        /*
        Case #1: when remainingQuota - buffer is less than lastApiCallsUsed,
        Don't auto refresh until next apiQuota renewal
        */
        result = RefreshTimer.computeRefreshTimerPeriod(201, 35, 25, 200, 1);
        assertEquals(result, 36);

        result = RefreshTimer.computeRefreshTimerPeriod(224, 35, 25, 200, 1);
        assertEquals(result, 36);

        /*
        Case #2: when (remainingQuota - buffer) is more or equal than lastApiCallsUsed,
        Calculate the refreshTime as normal
        */
        result = RefreshTimer.computeRefreshTimerPeriod(225, 35, 25, 200, 1);
        assertEquals(result, 36);

        result = RefreshTimer.computeRefreshTimerPeriod(226, 35, 25, 200, 1);
        assertEquals(result, 36);

        result = RefreshTimer.computeRefreshTimerPeriod(3000, 35, 25, 200, 5);
        assertEquals(result, 5);

        result = RefreshTimer.computeRefreshTimerPeriod(3000, 35, 223, 0, 1);
        assertEquals(result, 3);
    }

    @Test
    public void computeTickerTimer_noOfRefreshEqualOne() {
        long result = RefreshTimer.computeRefreshTimerPeriod(886, 9, 502, 200, 1);
        assertEquals(result, 10);
    }

    @Test
    public void computeTickerTimer_remainingTimeEqualZero() {
        long result = RefreshTimer.computeRefreshTimerPeriod(0, 0, 502, 1, 2);
        assertEquals(result, 2);
    }
}
