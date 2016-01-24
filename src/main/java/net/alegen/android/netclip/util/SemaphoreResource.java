package net.alegen.android.netclip.util;

import android.util.Log;

import java.lang.StackTraceElement;
import java.lang.Thread;

import java.util.concurrent.Semaphore;

public class SemaphoreResource<T> {

    private T resource;
    private String tag;
    private Semaphore semaphore;

    public SemaphoreResource(T resource, String tag) {
        this.resource = resource;
        this.tag = tag;
        this.semaphore = new Semaphore(1, true);
    }

    public T acquire(String... l) {
        Log.i("netclip", "SemaphoreResource.acquire - resource tag is " + this.tag);
        for (String s : l)
            Log.i("netclip", s);
        try {
            this.semaphore.acquire();
            Log.i("netclip", "SemaphoreResource.acquire - semaphore acquired");
        } catch (InterruptedException ex) {
            Log.e("netclip", "SemaphoreResource.acquire - exception caught - " + ex.getMessage());
        }
        return this.resource;
    }

    public void release() {
        Log.i("netclip", "SemaphoreResource.release - resource tag is " + this.tag);
        this.semaphore.release();
        Log.i("netclip", "SemaphoreResource.acquire - semaphore released");
    }
}
