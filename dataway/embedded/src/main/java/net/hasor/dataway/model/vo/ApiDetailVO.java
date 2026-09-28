/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model.vo;
import java.util.Map;

/** Editable API details and metadata displayed by the management console. */
public class ApiDetailVO {
    private String              id;
    private long                version;
    private String              select;
    private String              path;
    private int                 status;
    private String              apiComment;
    private String              codeType;
    private ApiCodeVO           codeInfo;
    private String              requestBody;
    private Object              headerData;
    private Map<String, Object> optionData;
    private Map<String, Object> sample;
    private Map<String, Object> schema;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return this.version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getSelect() {
        return this.select;
    }

    public void setSelect(String select) {
        this.select = select;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getApiComment() {
        return this.apiComment;
    }

    public void setApiComment(String apiComment) {
        this.apiComment = apiComment;
    }

    public String getCodeType() {
        return this.codeType;
    }

    public void setCodeType(String codeType) {
        this.codeType = codeType;
    }

    public ApiCodeVO getCodeInfo() {
        return this.codeInfo;
    }

    public void setCodeInfo(ApiCodeVO codeInfo) {
        this.codeInfo = codeInfo;
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

    public Map<String, Object> getOptionData() {
        return this.optionData;
    }

    public void setOptionData(Map<String, Object> optionData) {
        this.optionData = optionData;
    }

    public Map<String, Object> getSample() {
        return this.sample;
    }

    public void setSample(Map<String, Object> sample) {
        this.sample = sample;
    }

    public Map<String, Object> getSchema() {
        return this.schema;
    }

    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }
}
