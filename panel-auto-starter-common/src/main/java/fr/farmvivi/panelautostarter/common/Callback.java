package fr.farmvivi.panelautostarter.common;

/**
 * Represents a method which may be called once a result has been computed
 * asynchronously.
 *
 * @param <V> the type of result
 */
public interface Callback<V> {
    /**
     * Called when the result is done.
     *
     * @param result the result of the computation
     */
    void done(V result);
}
