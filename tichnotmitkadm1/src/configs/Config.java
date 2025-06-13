package configs;
/**
 * Config is an interface for any configuration source that can supply
 * a mapping of initial numeric nodes and a mapping of expressions
 * for building a computational graph.
 */

public interface Config {

    void create();
    String getName();
    int getVersion();
    void close();
}
