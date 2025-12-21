package dev.sakura.utils.time;

public class TimerUtil {
    public long lastMS;

    private long lastTime = 0;

    public boolean delay(float ticks) {
        return getCurrentMS() - lastMS >= ticks * 50;
    }

    public long getCurrentMS() {
        return System.nanoTime() / 1000000L;
    }

    public boolean hasReached(double milliseconds) {
        if (milliseconds == 0) {
            return true;
        }
        return (double) (this.getCurrentMS() - this.lastMS) >= milliseconds;
    }

    public boolean hasTimeElapsed(long time) {
        return getCurrentMS() - this.lastMS > time;
    }

    public void reset() {
        this.lastMS = this.getCurrentMS();
    }

    public long passed() {
        return this.getCurrentMS() - this.lastMS;
    }

    public long getTime() {
        return getCurrentMS() - this.lastMS;
    }

    public void setTime(long time) {
        this.lastMS = time;
    }
}
