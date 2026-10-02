/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.result;
import java.util.Map;

/** Execution outcome and resolved handler options supplied to a result handler. */
public class ResultContext {
    private boolean             success;
    private int                 code;
    private String              message;
    private String              location;
    private long                lifeCycleTime;
    private long                executionTime;
    private Object              value;
    private Exception           error;
    private Map<String, Object> options;

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getLifeCycleTime() {
        return this.lifeCycleTime;
    }

    public void setLifeCycleTime(long lifeCycleTime) {
        this.lifeCycleTime = lifeCycleTime;
    }

    public long getExecutionTime() {
        return this.executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Exception getError() {
        return this.error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    public Map<String, Object> getOptions() {
        return this.options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
}
