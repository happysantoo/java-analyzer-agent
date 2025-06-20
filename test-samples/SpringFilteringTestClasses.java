package com.example.scanner.test;

import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test classes for Spring annotation filtering.
 * These classes contain concurrency issues and Spring annotations.
 */
public class SpringFilteringTestClasses {

    /**
     * Service class with concurrency issues - should be analyzed when filtering is enabled.
     */
    @Service
    public static class UserService {
        private Map<String, String> userCache = new HashMap<>(); // Concurrency issue: HashMap is not thread-safe
        private int userCount = 0; // Concurrency issue: non-atomic increment
        
        public void addUser(String id, String name) {
            userCache.put(id, name); // Race condition possible
            userCount++; // Race condition possible
        }
        
        public String getUser(String id) {
            return userCache.get(id);
        }
        
        public int getUserCount() {
            return userCount;
        }
    }

    /**
     * Component class with concurrency issues - should be analyzed when filtering is enabled.
     */
    @Component
    public static class DataProcessor {
        private List<String> processedData = new ArrayList<>(); // Concurrency issue: ArrayList is not thread-safe
        
        public synchronized void processData(String data) {
            // Synchronization on 'this' - potential issue
            processedData.add(data.toUpperCase());
        }
        
        public void clearData() {
            processedData.clear(); // Race condition if called concurrently with processData
        }
    }

    /**
     * Repository class with good concurrency practices - should be analyzed when filtering is enabled.
     */
    @Repository
    public static class UserRepository {
        private final Map<String, String> userStorage = new ConcurrentHashMap<>(); // Good: thread-safe
        
        public void saveUser(String id, String data) {
            userStorage.put(id, data);
        }
        
        public String findUser(String id) {
            return userStorage.get(id);
        }
    }

    /**
     * Controller class with concurrency issues - should be analyzed when filtering is enabled.
     */
    @Controller
    public static class UserController {
        private static int requestCount = 0; // Concurrency issue: static field without synchronization
        
        public void handleRequest() {
            requestCount++; // Race condition
        }
        
        public static int getRequestCount() {
            return requestCount;
        }
    }

    /**
     * RestController class - should be analyzed when filtering is enabled.
     */
    @RestController
    public static class ApiController {
        private volatile boolean isProcessing = false; // Good: volatile for simple flag
        
        public void startProcessing() {
            if (!isProcessing) { // Check-then-act race condition
                isProcessing = true;
                // Do processing
            }
        }
    }

    /**
     * Configuration class - should be analyzed when filtering is enabled.
     */
    @Configuration
    public static class AppConfiguration {
        private Map<String, Object> configCache = new HashMap<>(); // Concurrency issue if accessed concurrently
        
        public void updateConfig(String key, Object value) {
            configCache.put(key, value);
        }
        
        public Object getConfig(String key) {
            return configCache.get(key);
        }
    }

    /**
     * Plain class WITHOUT Spring annotations - should NOT be analyzed when filtering is enabled.
     */
    public static class PlainUtilityClass {
        private static Map<String, String> cache = new HashMap<>(); // Concurrency issue but should be ignored when filtering
        private int counter = 0; // Concurrency issue but should be ignored when filtering
        
        public static void putInCache(String key, String value) {
            cache.put(key, value); // Race condition but should be ignored when filtering
        }
        
        public void increment() {
            counter++; // Race condition but should be ignored when filtering
        }
    }

    /**
     * Another plain class WITHOUT Spring annotations - should NOT be analyzed when filtering is enabled.
     */
    public static class AnotherPlainClass {
        private List<String> data = new ArrayList<>(); // Thread-safety issue but should be ignored when filtering
        
        public void addData(String item) {
            data.add(item); // Should be ignored when filtering
        }
        
        public List<String> getData() {
            return data; // Should be ignored when filtering
        }
    }
}
