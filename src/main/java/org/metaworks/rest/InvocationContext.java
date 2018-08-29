package org.metaworks.rest;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Created by uengine on 2017. 6. 18..
 */
@Data
@NoArgsConstructor
public class InvocationContext {
    private String objectTypeName;
    private Object clientObject;
    private String methodName;
    private Map<String, Object> autowiredFields;
}
