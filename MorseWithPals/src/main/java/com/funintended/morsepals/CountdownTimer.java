package com.funintended.morsepals;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by dustin on 1/20/14. :)
 */
public class CountdownTimer {

    public interface CountdownTimerListener {
        void onSecondsTicked(int remainingSeconds);
        void onTimerStarted();
        void onTimerFinished();
    }

    protected Counter mRunnable = new Counter();

    private int mTotalSeconds;
    private CountdownTimerListener mCountdownTimerListener;


    protected long mTickFrequency = 1;
    protected ScheduledExecutorService mExecutor = Executors.newScheduledThreadPool(1);
    protected ScheduledFuture counterFuture;

    public CountdownTimer(int secondsDuration, CountdownTimerListener listener) {
        mTotalSeconds = secondsDuration;
        mCountdownTimerListener = listener;
    }

    public void start() {
        stop();
        mRunnable.setSeconds(mTotalSeconds);
        scheduleTicker();
        notifyTimeChange(mRunnable.seconds);
        if (mCountdownTimerListener != null) {
            mCountdownTimerListener.onTimerStarted();
        }
    }

    public void pause() {
        stop();
    }

    public void resume() {
        notifyTimeChange(mRunnable.getSeconds());
        scheduleTicker();
    }

    private void stop() {
        if (counterFuture != null) {
            counterFuture.cancel(true);
            counterFuture = null;
        }
        mExecutor.shutdown();
        mExecutor = Executors.newScheduledThreadPool(1);
    }

    private void scheduleTicker() {
        if (counterFuture != null) {
            counterFuture.cancel(true);
            counterFuture = null;
        }
        counterFuture = mExecutor.scheduleAtFixedRate(mRunnable, mTickFrequency, mTickFrequency, TimeUnit.SECONDS);
    }

    public void setTimeSeconds(int seconds) {
        mRunnable.setSeconds(seconds);
        notifyTimeChange(mRunnable.seconds);
    }

    private void notifyTimeChange(int seconds) {
        if (mCountdownTimerListener != null) {
            mCountdownTimerListener.onSecondsTicked(seconds);
        }
        if (seconds <= 0) {
            if (mCountdownTimerListener != null) {
                mCountdownTimerListener.onTimerFinished();
            }
            stop();
        }
    }

    public int getCurrentSeconds() {
        return mRunnable.getSeconds();
    }

    private class Counter implements Runnable {

        private int seconds;

        public Counter() {
        }

        @Override
        public void run() {
            seconds--;
            notifyTimeChange(seconds);
        }

        private int getSeconds() {
            return seconds;
        }

        private void setSeconds(int seconds) {
            this.seconds = seconds;
        }
    }
}
