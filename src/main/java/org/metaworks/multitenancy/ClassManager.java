package org.metaworks.multitenancy;

import org.metaworks.WebFieldDescriptor;
import org.metaworks.WebObjectType;
import org.metaworks.annotation.Id;
import org.metaworks.annotation.ServiceMethod;
import org.metaworks.dwr.MetaworksRemoteService;
import org.oce.garuda.multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.uengine.uml.model.Attribute;
import org.uengine.uml.model.ClassDefinition;


/**
 * Created by uengine on 2017. 5. 8..
 */
@Component
public class ClassManager {

    ClassDefinition classDefinition;
        public ClassDefinition getClassDefinition() {
            return classDefinition;
        }
        public void setClassDefinition(ClassDefinition classDefinition) {
            this.classDefinition = classDefinition;
        }

    String className;
    @Id
        public String getClassName() {
            return className;
        }
        public void setClassName(String className) {
            this.className = className;
        }

    @ServiceMethod(callByContent = true)
    public void save() throws Exception {
        metadataService.setClassDefinition(getClassDefinition(), TenantContext.getThreadLocalInstance().getTenantId());
    }

    @ServiceMethod
    public void loadOriginal() throws Exception {
        WebObjectType webObjectType = MetaworksRemoteService.getInstance().getMetaworksType(getClassName());

        setClassDefinition(new ClassDefinition());
        classDefinition.setFieldDescriptors(new Attribute[webObjectType.getFieldDescriptors().length]);

        int i=0;
        for(WebFieldDescriptor fieldDescriptor : webObjectType.getFieldDescriptors()){

            Attribute attribute = new Attribute();
            org.springframework.beans.BeanUtils.copyProperties(fieldDescriptor, attribute);
            attribute.setAttributes(null); //TODO: this occurs some Serialization error for Map<String, Object> for boolean value

            classDefinition.getFieldDescriptors()[i] = attribute;

            i++;
        }

        classDefinition.setName(webObjectType.getName());

    }

    @Autowired
    public MetadataService metadataService;

    @ServiceMethod
    public void load() throws Exception {
        ClassDefinition classDefinition = metadataService.getClassDefinition(Thread.currentThread().getContextClassLoader().loadClass(getClassName()), TenantContext.getThreadLocalInstance().getTenantId());

        setClassDefinition(classDefinition);
    }



}
