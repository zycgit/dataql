/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model.vo;

/** Script content and request examples displayed by the management console. */
public class ApiCodeVO {
    private String codeValue;
    private String requestBody;
    private Object headerData;

    public String getCodeValue() {
        return this.codeValue;
    }

    public void setCodeValue(String codeValue) {
        this.codeValue = codeValue;
    }

    public String getRequestBody() {
        return this.requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Object getHeaderData() {
        return this.headerData;
    }

    public void setHeaderData(Object headerData) {
        this.headerData = headerData;
    }
}
