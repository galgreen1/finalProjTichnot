package graph;
/**
 * Agent is the interface for any node in the directed acyclic computational graph.
 * Each Agent can compute() a double value, and has a unique name.
 */

public interface Agent {

    String getName();
    void reset();
    void callback(String topic, Message msg);
    void close();
}
