package com.github.weaselworks.myo.driver;

import org.thingml.bglib.BGAPI;
import org.thingml.bglib.BGAPIDefaultListener;

import java.util.concurrent.*;

/**
 * This is a custom Future which transforms the send/response callback nature of the writeAttr
 * in the BGAPI api into a blocking call. Returns true if the write was successful.
 */
public class BgapiWriteAttrFuture extends BGAPIDefaultListener implements Future<Boolean> {

    private volatile BGAPI client = null;
    private volatile Boolean result = null;
    private volatile boolean cancelled = false;
    private final CountDownLatch countDownLatch;
    private volatile int conn;
    private volatile int handle;
    private volatile byte[] data;

    public BgapiWriteAttrFuture(BGAPI client, int conn, int handle, byte[] data)
    {
        this.conn = conn;
        this.handle = handle;
        this.data = data;
        this.client = client;
        this.client.addListener(this);
        countDownLatch = new CountDownLatch(1);
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        if (isDone()) {
            return false;
        } else {
            countDownLatch.countDown();
            cancelled = true;
            tidyUp();
            return !isDone();
        }
    }

    @Override
    public Boolean get() throws InterruptedException, ExecutionException {
        doAttrWrite();
        countDownLatch.await();
        tidyUp();
        return result;
    }

    @Override
    public Boolean get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        doAttrWrite();
        countDownLatch.await(timeout, unit);
        tidyUp();
        return result;
    }

    private void doAttrWrite() {
        client.send_attclient_attribute_write(conn, handle, data);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return countDownLatch.getCount() == 0;
    }


    @Override
    public void receive_attclient_procedure_completed(int connection, int result, int chrhandle){
        this.result = result == 0;
        countDownLatch.countDown();
    }

    private void tidyUp() {
        this.client.removeListener(this);
    }

}
