package net.rideshare_ptc;


import java.util.concurrent.atomic.AtomicLong;

interface Clock {

    public long getCurrentMillis();

    public void sleep(long millis) throws InterruptedException;

}

class SystemClock implements Clock {

    @Override
    public long getCurrentMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

}

class MockClock implements Clock {

    private final AtomicLong currentTime = new AtomicLong(0);


    public MockClock() {
        this(System.currentTimeMillis());
    }

    public MockClock(long currentTime) {
        this.currentTime.set(currentTime);
    }


    @Override
    public long getCurrentMillis() {
        return currentTime.addAndGet(5);
    }

    @Override
    public void sleep(long millis) {
        currentTime.addAndGet(millis);
    }

}
