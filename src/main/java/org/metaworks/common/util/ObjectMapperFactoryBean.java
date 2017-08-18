package org.metaworks.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ObjectMapperFactoryBean implements FactoryBean<ObjectMapper>, InitializingBean {

    private ObjectMapper objectMapper;

    private boolean isIndentOutput = false;

    private boolean failOnUnknownProperties = true;

    @Override
    public ObjectMapper getObject() throws Exception {
        return this.objectMapper;
    }

    @Override
    public Class<ObjectMapper> getObjectType() {
        return ObjectMapper.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.objectMapper = new ObjectMapper();

        if (isIndentOutput) {
            this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        }

        objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, this.failOnUnknownProperties);

    }

    public void setIndentOutput(boolean isIndentOutput) {
        this.isIndentOutput = isIndentOutput;
    }

    public void setFailOnUnknownProperties(boolean failOnUnknownProperties) {
        this.failOnUnknownProperties = failOnUnknownProperties;
    }
}
