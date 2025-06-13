package graph;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
/**
 * TopicManagerSingleton is a thread-safe singleton that manages all Topics in the system.
 * It ensures a single shared repository of named Topic instances. Each topic name
 * maps to exactly one Topic object.
 */

public class TopicManagerSingleton {
    /**
     * TopicManager manages all Topics in the system.
     * Each topic name
     maps to exactly one Topic object.
     */
    // The TopicManager class
    public static class TopicManager {
        private final ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();


        // Private constructor to prevent creation of additional instances
        private TopicManager() { }

        // Map that holds the topics
        public Topic getTopic(String name) {
            return topics.computeIfAbsent(name, k -> new Topic(k));
        }

        // Returns all the topics
        public Collection<Topic> getTopics() {
            return topics.values();
        }

        // Clears all the topics
        public void clear() {
            topics.clear();
        }
    }

    // Static instance of TopicManager (Singleton)
    private static volatile TopicManager instance;

    // Static method to return the unique instance of TopicManager
    public static TopicManager get() {
        if (instance == null) {
            synchronized (TopicManagerSingleton.class) {
                if (instance == null) {
                    instance = new TopicManager();
                }
            }
        }
        return instance;
    }
}
