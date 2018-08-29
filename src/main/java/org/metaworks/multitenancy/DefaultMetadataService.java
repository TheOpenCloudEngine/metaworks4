package org.metaworks.multitenancy;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import lombok.Data;
import org.boon.Boon;
import org.metaworks.FieldDescriptor;
import org.metaworks.ObjectInstance;
import org.metaworks.WebFieldDescriptor;
import org.metaworks.WebObjectType;
import org.metaworks.dwr.MetaworksRemoteService;
import org.uengine.modeling.resource.DefaultResource;
import org.uengine.modeling.resource.IResource;
import org.uengine.modeling.resource.ResourceManager;
import org.uengine.modeling.resource.Serializer;
import org.uengine.uml.model.Attribute;
import org.uengine.uml.model.ClassDefinition;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Created by uengine on 2017. 5. 7..
 */
@Data
public class DefaultMetadataService implements MetadataService {

    ResourceManager resourceManager;

    @Override
    public <T> T getMetadata(Class<T> clazz, String tenantId) throws Exception {

        String metadataClassName = clazz.getName();

        IResource resource = new DefaultResource("md_" + tenantId + "_" + metadataClassName);

        T metadata = (T) getResourceManager().getObject(resource);

        return metadata;

    }

    @Override
    public void setMetadata(Object metadata, String tenantId) throws Exception {

        String metadataClassName = metadata.getClass().getName();

        IResource resource = new DefaultResource("md_" + tenantId + "_" + metadataClassName);

        getResourceManager().save(resource, metadata);


    }

    @Override
    public ClassDefinition getClassDefinition(Class clazz, String tenantId) throws Exception {

        String className = clazz.getName();

        //load class definition in the original form (java) first
        ClassDefinition classDefinition = (new ClassDefinition());
        List<WebFieldDescriptor> attributeList = new ArrayList<>();
        {
            WebObjectType webObjectType = MetaworksRemoteService.getInstance().getMetaworksType(className);

            classDefinition.setFieldDescriptors(new Attribute[webObjectType.getFieldDescriptors().length]);

            int i = 0;
            for (WebFieldDescriptor fieldDescriptor : webObjectType.getFieldDescriptors()) {

                Attribute attribute = new Attribute();
                org.springframework.beans.BeanUtils.copyProperties(fieldDescriptor, attribute);
                //attribute.setAttributes(null); //TODO: this occurs some Serialization error for Map<String, Object> for boolean value

                classDefinition.getFieldDescriptors()[i] = attribute;

                i++;

                if (attribute.getAttributes() != null)
                    attribute.getAttributes().remove("extended");

                attributeList.add(attribute);
            }

            classDefinition.setServiceMethodContexts(webObjectType.getServiceMethodContexts());
            classDefinition.setName(clazz.getName());
            classDefinition.setDisplayName(webObjectType.getDisplayName());
            classDefinition.setKeyFieldDescriptor(webObjectType.getKeyFieldDescriptor());

        }

        IResource resource = new DefaultResource("clsDef_" + tenantId + "_" + className);

        try {
            ClassDefinition overriderClassDefinition = (ClassDefinition) getResourceManager().getObject(resource);

            if (overriderClassDefinition.getFieldDescriptors() != null)
                for (WebFieldDescriptor attribute : overriderClassDefinition.getFieldDescriptors()) {
                    if (!attributeList.contains(attribute)) {
                        attribute.setAttributes(new Properties());
                        attribute.getAttributes().put("extended", "true");
                        attributeList.add(attribute);
                        if (attribute.getDisplayName() == null || attribute.getDisplayName().trim().length() == 0)
                            attribute.setDisplayName(attribute.getName());
                    } else {

                    }
                }

            Attribute[] attributes = new Attribute[attributeList.size()];
            attributeList.toArray(attributes);
            classDefinition.setFieldDescriptors(attributes);

        } catch (FileNotFoundException fne) {

        } catch (NullPointerException npe) {

        }

        return classDefinition;
    }

    @Override
    public void setClassDefinition(ClassDefinition classDefinition, String tenantId) throws Exception {
        String className = classDefinition.getName();

        IResource resource = new DefaultResource("clsDef_" + tenantId + "_" + className);

        getResourceManager().save(resource, classDefinition);
    }

}
