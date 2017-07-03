package org.metaworks.multitenancy;

import org.springframework.beans.factory.annotation.Autowired;
import org.uengine.uml.model.ClassDefinition;

/**
 * Created by uengine on 2017. 5. 7..
 */
public interface MetadataService {

    public <T> T getMetadata(Class<T> clazz, String tenantId) throws Exception;

    public void setMetadata(Object metadata, String tenantId) throws Exception;


    public ClassDefinition getClassDefinition(Class clazz, String tenantId) throws Exception;
    public void setClassDefinition(ClassDefinition classDefinition, String tenantId) throws Exception;
}
