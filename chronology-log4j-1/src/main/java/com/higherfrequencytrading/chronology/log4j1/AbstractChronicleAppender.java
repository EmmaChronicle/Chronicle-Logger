package com.higherfrequencytrading.chronology.log4j1;

import com.higherfrequencytrading.chronology.ChronologyLogLevel;
import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ExcerptAppender;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OnlyOnceErrorHandler;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;

import java.io.IOException;

public abstract class AbstractChronicleAppender implements Appender, OptionHandler {

    private Filter filter;
    private String name;
    private ErrorHandler errorHandler;

    protected Chronicle chronicle;
    protected ExcerptAppender appender;

    private String path;

    protected AbstractChronicleAppender() {
        this.path = null;
        this.chronicle = null;
        this.appender = null;
        this.name = null;
        this.errorHandler = new OnlyOnceErrorHandler();
    }

    // *************************************************************************
    // Custom logging options
    // *************************************************************************

    @Override
    public void activateOptions() {
        if(path != null) {
            createAppender();
        } else {
            LogLog.warn("path option not set for appender ["+name+"].");
        }
    }

    @Override
    public void addFilter(Filter newFilter) {
        if(filter == null) {
            filter = newFilter;
        } else {
            filter.setNext(newFilter);
        }
    }

    @Override
    public void clearFilters() {
        filter = null;
    }

    // *************************************************************************
    // Custom logging options
    // *************************************************************************

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public void finalize() {
        // An appender might be closed then garbage collected. There is no
        // point in closing twice.
        if(this.chronicle == null) {
            LogLog.debug("Finalizing appender named [" + name + "].");
            close();
        }
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    @Override
    public Filter getFilter() {
        return this.filter;
    }

    @Override
    public Layout getLayout() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public synchronized void setErrorHandler(ErrorHandler eh) {
        if(eh == null) {
            // We do not throw exception here since the cause is probably a
            // bad config file.
            LogLog.warn("You have tried to set a null error-handler.");
        } else {
            this.errorHandler = eh;
        }
    }

    @Override
    public void setLayout(Layout layout) {
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void doAppend(LoggingEvent event) {
        if(this.chronicle != null) {
            for(Filter f = this.filter; f != null;f = f.getNext()) {
                switch(f.decide(event)) {
                    case Filter.DENY:
                        return;
                    case Filter.ACCEPT:
                        f = null;
                        break;
                }
            }

            append(event);
        } else {
            LogLog.error("Attempted to append to closed appender named ["+name+"].");
            return;
        }
    }

    // *************************************************************************
    // Chronicle implementation
    // *************************************************************************

    protected abstract void append(LoggingEvent event);

    protected abstract Chronicle createChronicle() throws IOException;

    protected void createAppender() {
        if(this.chronicle == null) {
            try {
                this.chronicle = createChronicle();
                this.appender  = this.chronicle.createAppender();
            } catch(IOException e) {
                //TODO: manage exception
                this.chronicle = null;
                this.appender  = null;
            }
        }
    }

    // *************************************************************************
    //
    // *************************************************************************

    @Override
    public void close() {
        if(this.chronicle != null) {
            try {
                if(this.appender != null) {
                    this.appender.close();
                }

                if(this.chronicle != null) {
                    this.chronicle.close();
                }
            } catch(IOException e) {
                //TODO: manage exception
            }
        }
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    // *************************************************************************
    //
    // *************************************************************************

    public static int toIntChronologyLogLevel(final Level level) {
        switch(level.toInt()) {
            case Level.DEBUG_INT:
                return ChronologyLogLevel.DEBUG.levelInt;
            case Level.TRACE_INT:
                return ChronologyLogLevel.TRACE.levelInt;
            case Level.INFO_INT:
                return ChronologyLogLevel.INFO.levelInt;
            case Level.WARN_INT:
                return ChronologyLogLevel.WARN.levelInt;
            case Level.ERROR_INT:
                return ChronologyLogLevel.ERROR.levelInt;
            default:
                throw new IllegalArgumentException(level.toInt() + " not a valid level value");
        }
    }

    public static String toStrChronologyLogLevel(final Level level) {
        switch(level.toInt()) {
            case Level.DEBUG_INT:
                return ChronologyLogLevel.DEBUG.levelStr;
            case Level.TRACE_INT:
                return ChronologyLogLevel.TRACE.levelStr;
            case Level.INFO_INT:
                return ChronologyLogLevel.INFO.levelStr;
            case Level.WARN_INT:
                return ChronologyLogLevel.WARN.levelStr;
            case Level.ERROR_INT:
                return ChronologyLogLevel.ERROR.levelStr;
            default:
                throw new IllegalArgumentException(level.toInt() + " not a valid level value");
        }
    }
}
