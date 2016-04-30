package tests;

import org.junit.Test;
import util.TickingTimer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TickingTimerTests {

    /**
     * Facilitates simulation of the triggering of timer for changePeriodTest().
     */
    Boolean isTriggered = false;

    private static void delay(double seconds) {
        int time = (int) (seconds * 1000);
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static TickingTimer createTickingTimer() {
        return new TickingTimer("test", 10, (i) -> {
        }, () -> {
        }, TimeUnit.SECONDS);
    }

    @Test(expected = AssertionError.class)
    public void doublePauseTest() {
        final TickingTimer tickingTimer = createTickingTimer();
        tickingTimer.start();
        tickingTimer.pause();
        tickingTimer.pause();
    }

    @Test
    public void threadingTest() {
        final int attempts = 2;

        final TickingTimer tickingTimer = createTickingTimer();
        tickingTimer.start();
        ExecutorService es = Executors.newFixedThreadPool(2);
        Runnable positive = () -> {
            for (int i = 0; i < attempts; i++) {
                tickingTimer.pause();
                delay(2);
            }
        };
        Runnable negative = () -> {
            for (int i = 0; i < attempts; i++) {
                tickingTimer.resume();
                delay(2);
            }
        };
        es.execute(positive);
        delay(1);
        es.execute(negative);
        delay(4.5); // Need 6 seconds to wait for the positive and negative runnables to complete the run.
    }

    @Test
    public void triggerTest() {
        final ArrayList<Integer> ticks = new ArrayList<>();
        final ArrayList<Integer> ideal = new ArrayList<>(Arrays.asList(
                4, 3, // After 1.5
                5, 10, // Trigger
                4, 3)); // After 3.5

        // Timeouts every five seconds, tick every second
        final TickingTimer tickingTimer = new TickingTimer("test2", 5, ticks::add, () -> {
            ticks.add(10);
        }, TimeUnit.SECONDS);

        tickingTimer.start();
        delay(1.5);
        tickingTimer.trigger();
        delay(3.5);
        tickingTimer.stop();

        for (int i = 0; i < ideal.size(); i++) {
            if (ticks.get(i) != ideal.get(i)) {
                fail();
            }
        }
    }

    @Test
    public void changePeriodTest() {

        final ArrayList<Integer> ticks = new ArrayList<>();

        final TickingTimer tickingTimer = new TickingTimer("test3", 5, (Integer i) -> {}, () -> {
            isTriggered = true;
            ticks.clear();
        }, TimeUnit.SECONDS);

        tickingTimer.start();
        delay(3.9); //TickingTimer initial timeout is 1 sec less.
        assertTrue(!hasTimeout());
        tickingTimer.restartTimer(3);
        delay(1.5);
        //At t=1.5s, timeout should not be called yet.
        assertTrue(!hasTimeout());
        delay(1.6);
        //At t=3.1s, timeout should be called already.
        assertTrue(hasTimeout());
        resetTimeoutStatus();

        //Second iteration after calling restartTimer method.
        delay(2);
        //At t=2.1s, timeout should not be called yet.
        assertTrue(!hasTimeout());
        delay(1.5);
        //At t=3.6s, timeout should be called already.
        assertTrue(hasTimeout());
    }

    private void resetTimeoutStatus() {
        isTriggered = false;
    }

    private boolean hasTimeout() {
        return isTriggered;
    }
}
