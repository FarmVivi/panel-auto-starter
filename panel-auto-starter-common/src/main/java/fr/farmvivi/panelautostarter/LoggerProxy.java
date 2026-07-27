package fr.farmvivi.panelautostarter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;

import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerProxy extends Logger {
    private final Logger delegate;

    public LoggerProxy(Logger logger) {
        super(logger.getName(), logger.getResourceBundleName());
        this.delegate = logger;
    }

    @Override
    public ResourceBundle getResourceBundle() {
        return delegate.getResourceBundle();
    }

    @Override
    public void setResourceBundle(ResourceBundle bundle) {
        delegate.setResourceBundle(bundle);
    }

    @Override
    public String getResourceBundleName() {
        return delegate.getResourceBundleName();
    }

    @Override
    public Filter getFilter() {
        return delegate.getFilter();
    }

    @Override
    public void setFilter(Filter newFilter) throws SecurityException {
        delegate.setFilter(newFilter);
    }

    @Override
    public void log(LogRecord logRecord) {
        delegate.log(logRecord);
    }

    @Override
    public void log(Level level, String msg) {
        if (delegate.isLoggable(level)) {
            delegate.log(level, msg);
        }
    }

    public void log(Level level, Component component) {
        if (delegate.isLoggable(level)) {
            delegate.log(level, convertComponentToLogacyComponent(component));
        }
    }

    @Override
    public void log(Level level, Supplier<String> msgSupplier) {
        if (delegate.isLoggable(level)) {
            delegate.log(level, msgSupplier);
        }
    }

    @Override
    public void log(Level level, String msg, Object param1) {
        if (delegate.isLoggable(level)) {
            delegate.log(level, msg, param1);
        }
    }

    public void log(Level level, Component component, Object param1) {
        if (delegate.isLoggable(level)) {
            delegate.log(level, convertComponentToLogacyComponent(component), param1);
        }
    }

    @Override
    public void log(Level level, String msg, Object[] params) {
        if (delegate.isLoggable(level)) {
            delegate.log(level, msg, params);
        }
    }

    public void log(Level level, Component component, Object[] params) {
        if (delegate.isLoggable(level)) {
            delegate.log(level, convertComponentToLogacyComponent(component), params);
        }
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
        if (delegate.isLoggable(level)) {
            delegate.log(level, msg, thrown);
        }
    }

    public void log(Level level, Component component, Throwable thrown) {
        if (delegate.isLoggable(level)) {
            delegate.log(level, convertComponentToLogacyComponent(component), thrown);
        }
    }

    @Override
    public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
        if (delegate.isLoggable(level)) {
            delegate.log(level, thrown, msgSupplier);
        }
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
        delegate.logp(level, sourceClass, sourceMethod, msg);
    }

    public void logp(Level level, String sourceClass, String sourceMethod, Component component) {
        delegate.logp(level, sourceClass, sourceMethod, convertComponentToLogacyComponent(component));
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, Supplier<String> msgSupplier) {
        delegate.logp(level, sourceClass, sourceMethod, msgSupplier);
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {
        delegate.logp(level, sourceClass, sourceMethod, msg, param1);
    }

    public void logp(Level level, String sourceClass, String sourceMethod, Component component, Object param1) {
        delegate.logp(level, sourceClass, sourceMethod, convertComponentToLogacyComponent(component), param1);
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object[] params) {
        delegate.logp(level, sourceClass, sourceMethod, msg, params);
    }

    public void logp(Level level, String sourceClass, String sourceMethod, Component component, Object[] params) {
        delegate.logp(level, sourceClass, sourceMethod, convertComponentToLogacyComponent(component), params);
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
        delegate.logp(level, sourceClass, sourceMethod, msg, thrown);
    }

    public void logp(Level level, String sourceClass, String sourceMethod, Component component, Throwable thrown) {
        delegate.logp(level, sourceClass, sourceMethod, convertComponentToLogacyComponent(component), thrown);
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, Throwable thrown, Supplier<String> msgSupplier) {
        if (delegate.isLoggable(level)) {
            delegate.logp(level, sourceClass, sourceMethod, thrown, msgSupplier);
        }
    }

    @Override
    public void entering(String sourceClass, String sourceMethod) {
        delegate.entering(sourceClass, sourceMethod);
    }

    @Override
    public void entering(String sourceClass, String sourceMethod, Object param1) {
        delegate.entering(sourceClass, sourceMethod, param1);
    }

    @Override
    public void entering(String sourceClass, String sourceMethod, Object[] params) {
        delegate.entering(sourceClass, sourceMethod, params);
    }

    @Override
    public void exiting(String sourceClass, String sourceMethod) {
        delegate.exiting(sourceClass, sourceMethod);
    }

    @Override
    public void exiting(String sourceClass, String sourceMethod, Object result) {
        delegate.exiting(sourceClass, sourceMethod, result);
    }

    @Override
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        delegate.throwing(sourceClass, sourceMethod, thrown);
    }

    @Override
    public void severe(String msg) {
        delegate.severe(msg);
    }

    public void severe(Component component) {
        if (delegate.isLoggable(Level.SEVERE)) {
            delegate.severe(convertComponentToLogacyComponent(component));
        }
    }

    @Override
    public void warning(String msg) {
        delegate.warning(msg);
    }

    public void warning(Component component) {
        if (delegate.isLoggable(Level.WARNING)) {
            delegate.warning(convertComponentToLogacyComponent(component));
        }
    }

    @Override
    public void info(String msg) {
        delegate.info(msg);
    }

    public void info(Component component) {
        if (delegate.isLoggable(Level.INFO)) {
            delegate.info(convertComponentToLogacyComponent(component));
        }
    }

    @Override
    public void config(String msg) {
        delegate.config(msg);
    }

    public void config(Component component) {
        if (delegate.isLoggable(Level.CONFIG)) {
            delegate.config(convertComponentToLogacyComponent(component));
        }
    }

    @Override
    public void fine(String msg) {
        delegate.fine(msg);
    }

    public void fine(Component component) {
        if (delegate.isLoggable(Level.FINE)) {
            delegate.fine(convertComponentToLogacyComponent(component));
        }
    }

    @Override
    public void finer(String msg) {
        delegate.finer(msg);
    }

    public void finer(Component component) {
        if (delegate.isLoggable(Level.FINER)) {
            delegate.finer(convertComponentToLogacyComponent(component));
        }
    }

    @Override
    public void finest(String msg) {
        delegate.finest(msg);
    }

    public void finest(Component component) {
        if (delegate.isLoggable(Level.FINEST)) {
            delegate.finest(convertComponentToLogacyComponent(component));
        }
    }

    @Override
    public void severe(Supplier<String> msgSupplier) {
        delegate.severe(msgSupplier);
    }

    @Override
    public void warning(Supplier<String> msgSupplier) {
        delegate.warning(msgSupplier);
    }

    @Override
    public void info(Supplier<String> msgSupplier) {
        delegate.info(msgSupplier);
    }

    @Override
    public void config(Supplier<String> msgSupplier) {
        delegate.config(msgSupplier);
    }

    @Override
    public void fine(Supplier<String> msgSupplier) {
        delegate.fine(msgSupplier);
    }

    @Override
    public void finer(Supplier<String> msgSupplier) {
        delegate.finer(msgSupplier);
    }

    @Override
    public void finest(Supplier<String> msgSupplier) {
        delegate.finest(msgSupplier);
    }

    @Override
    public Level getLevel() {
        return delegate.getLevel();
    }

    @Override
    public void setLevel(Level newLevel) throws SecurityException {
        delegate.setLevel(newLevel);
    }

    @Override
    public boolean isLoggable(Level level) {
        return delegate.isLoggable(level);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void addHandler(Handler handler) throws SecurityException {
        delegate.addHandler(handler);
    }

    @Override
    public void removeHandler(Handler handler) throws SecurityException {
        delegate.removeHandler(handler);
    }

    @Override
    public Handler[] getHandlers() {
        return delegate.getHandlers();
    }

    @Override
    public boolean getUseParentHandlers() {
        return delegate.getUseParentHandlers();
    }

    @Override
    public void setUseParentHandlers(boolean useParentHandlers) {
        delegate.setUseParentHandlers(useParentHandlers);
    }

    @Override
    public Logger getParent() {
        return delegate.getParent();
    }

    @Override
    public void setParent(Logger parent) {
        delegate.setParent(parent);
    }

    public String convertComponentToLogacyComponent(Component component) {
        return ANSIComponentSerializer.ansi().serialize(component);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
