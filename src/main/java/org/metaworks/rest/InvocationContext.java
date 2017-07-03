package org.metaworks.rest;

import java.util.Map;

/**
 * Created by uengine on 2017. 6. 18..
 */
public class InvocationContext {
    private String objectTypeName;
    private Object clientObject;
    private String methodName;

    public Map<String, Object> getAutowiredFields() {
        return autowiredFields;
    }

    public void setAutowiredFields(Map<String, Object> autowiredFields) {
        this.autowiredFields = autowiredFields;
    }

    private Map<String, Object> autowiredFields;

    public String getObjectTypeName() {
        return objectTypeName;
    }

    public void setObjectTypeName(String objectTypeName) {
        this.objectTypeName = objectTypeName;
    }

    public Object getClientObject() {
        return clientObject;
    }

    public void setClientObject(Object clientObject) {
        this.clientObject = clientObject;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }


}
