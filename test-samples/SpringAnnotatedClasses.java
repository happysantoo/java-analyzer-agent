import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;

@Service
public class UserService {
    private AtomicInteger userCount = new AtomicInteger(0);
    private HashMap<String, String> userData = new HashMap<>(); // Thread safety issue
    
    public synchronized void addUser(String username, String data) {
        userCount.incrementAndGet();
        userData.put(username, data); // Potential concurrency issue
    }
    
    public int getUserCount() {
        return userCount.get();
    }
}

@Component
public class DataProcessor {
    private volatile boolean processing = false;
    private int counter = 0; // Race condition potential
    
    public synchronized void startProcessing() {
        processing = true;
        counter++; // Not thread-safe despite synchronized method
    }
    
    public boolean isProcessing() {
        return processing;
    }
}

@Repository
public class UserRepository {
    private HashMap<Long, String> cache = new HashMap<>(); // Thread safety issue
    
    public synchronized void cache(Long id, String data) {
        cache.put(id, data);
    }
    
    public String getFromCache(Long id) {
        return cache.get(id); // Not synchronized!
    }
}

public class PlainUtilityClass {
    private static int counter = 0; // Static field race condition
    
    public static void increment() {
        counter++; // Race condition
    }
    
    public static int getCounter() {
        return counter;
    }
}
