package com.example.test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sample Java class demonstrating good concurrency practices
 */
public class ThreadSafeExample {
    
    // Good: Using atomic integer for thread-safe counter
    private final AtomicInteger counter = new AtomicInteger(0);
    
    // Good: Using thread-safe collections
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> safeList = new CopyOnWriteArrayList<>();
    
    // Good: Proper use of volatile for flag
    private volatile boolean isShutdown = false;
    
    // Good: Immutable configuration
    private final String configValue;
    private final int maxSize;
    
    public ThreadSafeExample(String configValue, int maxSize) {
        this.configValue = configValue;
        this.maxSize = maxSize;
    }
    
    /**
     * Thread-safe counter increment using atomic operations
     */
    public int incrementAndGet() {
        return counter.incrementAndGet();
    }
    
    public int getCurrentCount() {
        return counter.get();
    }
    
    /**
     * Thread-safe list operations
     */
    public void addItem(String item) {
        if (!isShutdown) {
            safeList.add(item);
        }
    }
    
    public boolean removeItem(String item) {
        return safeList.remove(item);
    }
    
    /**
     * Thread-safe cache operations
     */
    public void putCacheValue(String key, Object value) {
        if (cache.size() < maxSize) {
            cache.put(key, value);
        }
    }
    
    public Object getCacheValue(String key) {
        return cache.get(key);
    }
    
    /**
     * Proper executor service usage with shutdown
     */
    public void processItemsConcurrently(java.util.List<String> items) {
        ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        
        try {
            CompletableFuture<?>[] futures = items.stream()
                .map(item -> CompletableFuture.runAsync(() -> processItem(item), executor))
                .toArray(CompletableFuture[]::new);
            
            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing failed", e);
        } finally {
            shutdownExecutor(executor);
        }
    }
    
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private void processItem(String item) {
        // Simulate processing
        System.out.println("Processing: " + item + " on thread: " + 
                          Thread.currentThread().getName());
    }
    
    /**
     * Thread-safe singleton using atomic reference
     */
    private static final AtomicReference<ThreadSafeExample> INSTANCE = 
        new AtomicReference<>();
    
    public static ThreadSafeExample getInstance(String config, int maxSize) {
        ThreadSafeExample instance = INSTANCE.get();
        if (instance == null) {
            ThreadSafeExample newInstance = new ThreadSafeExample(config, maxSize);
            if (INSTANCE.compareAndSet(null, newInstance)) {
                return newInstance;
            } else {
                return INSTANCE.get();
            }
        }
        return instance;
    }
    
    /**
     * Example of proper lock usage
     */
    private final ReentrantLock lock = new ReentrantLock();
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private String sharedData = "";
    
    public void updateData(String newData) {
        readWriteLock.writeLock().lock();
        try {
            sharedData = newData;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
    
    public String readData() {
        readWriteLock.readLock().lock();
        try {
            return sharedData;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
    
    /**
     * Graceful shutdown
     */
    public void shutdown() {
        isShutdown = true;
        cache.clear();
        safeList.clear();
    }
    
    public boolean isShutdown() {
        return isShutdown;
    }
}
