package com.example.test;

import java.util.*;
import java.util.concurrent.*;

/**
 * Sample Java class with various concurrency issues for testing the scanner
 */
public class ConcurrencyIssuesExample {
    
    // Issue 1: Non-thread-safe shared mutable state
    private static int sharedCounter = 0;
    private List<String> unsafeList = new ArrayList<>();
    
    // Issue 2: Non-final static field
    public static Map<String, Object> globalCache = new HashMap<>();
    
    // Issue 3: Potential race condition
    private boolean isInitialized = false;
    private String data;
    
    /**
     * Method with race condition - multiple threads can access and modify sharedCounter
     */
    public void incrementCounter() {
        // Race condition: read-modify-write operation not atomic
        sharedCounter++;
        
        // Another race condition with instance variable
        if (!isInitialized) {
            data = "initialized";
            isInitialized = true;
        }
    }
    
    /**
     * Method using non-thread-safe collection
     */
    public void addToUnsafeList(String item) {
        unsafeList.add(item); // ArrayList is not thread-safe
    }
    
    /**
     * Method with potential deadlock scenario
     */
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    public void methodA() {
        synchronized (lock1) {
            System.out.println("Method A acquired lock1");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (lock2) {
                System.out.println("Method A acquired lock2");
            }
        }
    }
    
    public void methodB() {
        synchronized (lock2) {
            System.out.println("Method B acquired lock2");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (lock1) {
                System.out.println("Method B acquired lock1");
            }
        }
    }
    
    /**
     * Method with improper use of volatile
     */
    private volatile boolean flag = false;
    private int counter = 0; // Should be volatile or atomic if accessed by multiple threads
    
    public void setFlag() {
        counter++; // Non-atomic operation on non-volatile field
        flag = true;
    }
    
    /**
     * Method with executor service not properly shutdown
     */
    public void processItems(List<String> items) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        for (String item : items) {
            executor.submit(() -> {
                processItem(item);
            });
        }
        
        // Missing: executor.shutdown() and awaitTermination()
    }
    
    private void processItem(String item) {
        // Simulate processing
        System.out.println("Processing: " + item);
    }
    
    /**
     * Method accessing global state without synchronization
     */
    public Object getCachedValue(String key) {
        return globalCache.get(key); // Accessing non-thread-safe HashMap
    }
    
    public void setCachedValue(String key, Object value) {
        globalCache.put(key, value); // Race condition possible
    }
    
    /**
     * Method with double-checked locking anti-pattern
     */
    private Object singleton;
    
    public Object getSingleton() {
        if (singleton == null) { // First check without synchronization
            synchronized (this) {
                if (singleton == null) { // Second check
                    singleton = new Object();
                }
            }
        }
        return singleton;
    }
}
