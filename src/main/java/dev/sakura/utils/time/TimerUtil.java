package dev.sakura.utils.time;

public class TimerUtil {
    public long lastMS;

    private long lastTime = 0;

    public boolean delay(float ticks) {
        return System.currentTimeMillis() - lastTime >= ticks * 50;
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
        return System.currentTimeMillis() - this.lastMS > time;
    }

    public void reset() {
        this.lastTime = System.currentTimeMillis();
        this.lastMS = this.getCurrentMS();
    }

    public long passed() {
        return this.getCurrentMS() - this.lastMS;
    }

    public long getTime() {
        return System.currentTimeMillis() - this.lastMS;
    }

    public void setTime(long time) {
        this.lastMS = time;
    }
}
