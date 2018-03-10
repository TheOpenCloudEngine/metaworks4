package org.metaworks.multitenancy;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import org.boon.Boon;
import org.metaworks.*;
import org.metaworks.dwr.MetaworksRemoteService;
import org.springframework.stereotype.Component;
import org.uengine.modeling.resource.Serializer;
import org.uengine.uml.model.Attribute;
import org.uengine.uml.model.ClassDefinition;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Created by uengine on 2017. 5. 7..
 */
public class CouchbaseMetadataService implements MetadataService {
    private String couchbaseServerIp;
    private String bucketPassword;

    public String getCouchbaseServerIp() {
            return couchbaseServerIp;
        }
        public void setCouchbaseServerIp(String couchbaseServerIp) {
            this.couchbaseServerIp = couchbaseServerIp;
        }

    String bucketName;
        public String getBucketName() {
            return bucketName;
        }
        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

    CouchbaseCluster cluster;


    protected Bucket getBucket(){
        return cluster.openBucket(getBucketName(), getBucketPassword());
    }


    @Override
    public <T> T getMetadata(Class<T> clazz, String tenantId) throws Exception{

        String metadataClassName = clazz.getName();

        WebObjectType webObjectType = MetaworksRemoteService.getInstance().getMetaworksType(metadataClassName);

        T metadata = clazz.newInstance();
        ObjectInstance metadataInstance = (ObjectInstance) webObjectType.metaworks2Type().createInstance();
        metadataInstance.setObject(metadata);


        for(FieldDescriptor fieldDescriptor : webObjectType.metaworks2Type().getFieldDescriptors()){
            String key;
            key = createKey(tenantId, metadataClassName, fieldDescriptor.getName());
            JsonDocument document = getBucket().get(key);

            if(document==null) continue;

            String json = document.content().getString("value");

            try{
                Object object = Boon.fromJson(json, fieldDescriptor.getClassType());

                metadataInstance.setFieldValue(fieldDescriptor.getName(), object);
            }catch (Exception e){

            }


        }



        return metadata;

    }

    @Override
    public void setMetadata(Object metadata, String tenantId) throws Exception{

        String metadataClassName = metadata.getClass().getName();

        WebObjectType webObjectType = MetaworksRemoteService.getInstance().getMetaworksType(metadataClassName);

        ObjectInstance metadataInstance = (ObjectInstance) webObjectType.metaworks2Type().createInstance();
        metadataInstance.setObject(metadata);

        for(FieldDescriptor fieldDescriptor : webObjectType.metaworks2Type().getFieldDescriptors()){

            Object value = metadataInstance.getFieldValue(fieldDescriptor.getName());

            String key;
            key = createKey(tenantId, metadataClassName, fieldDescriptor.getName());

            String json = Boon.toJson(value);

            JsonObject jsonObject = JsonObject.empty()
                    .put("value", json);

            JsonDocument stored = getBucket().upsert(JsonDocument.create(key, jsonObject));

        }

    }

    @Override
    public ClassDefinition getClassDefinition(Class clazz, String tenantId) throws Exception {

        String className = clazz.getName();

        //load class definition in the original form (java) first
        ClassDefinition classDefinition = (new ClassDefinition());
        List<Attribute> attributeList = new ArrayList<Attribute>();
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

                attributeList.add(attribute);
            }

            classDefinition.setServiceMethodContexts(webObjectType.getServiceMethodContexts());
            classDefinition.setName(clazz.getName());
            classDefinition.setDisplayName(webObjectType.getDisplayName());
            classDefinition.setKeyFieldDescriptor(webObjectType.getKeyFieldDescriptor());

        }

        String key = createKey(tenantId, clazz.getName(), "clsDef");
        JsonDocument document = getBucket().get(key);

        if(document==null) return classDefinition;

        String xml = document.content().getString("value");

        ClassDefinition overriderClassDefinition = (ClassDefinition) Serializer.deserialize(xml);

        if(overriderClassDefinition.getFieldDescriptors()!=null)
        for(Attribute attribute : overriderClassDefinition.getFieldDescriptors()){
            if(!attributeList.contains(attribute)){
                attribute.setAttributes(new Properties());
                attribute.getAttributes().put("extended", "true");
                attributeList.add(attribute);
                if(attribute.getDisplayName()==null || attribute.getDisplayName().trim().length()==0)
                    attribute.setDisplayName(attribute.getName());
            }else{

            }
        }

        Attribute[] attributes = new Attribute[attributeList.size()];
        attributeList.toArray(attributes);
        classDefinition.setFieldDescriptors(attributes);

        return classDefinition;
    }

    @Override
    public void setClassDefinition(ClassDefinition classDefinition, String tenantId) throws Exception {
        String key = createKey(tenantId, classDefinition.getName(), "clsDef");

        String xml = Serializer.serialize(classDefinition);
        JsonObject jsonObject = JsonObject.empty()
                .put("value", xml);


        getBucket().upsert(JsonDocument.create(key, jsonObject));
    }

    private String createKey(String tenantId, String metadataClassName, String propertyName) {

        return "_md_" + tenantId + "_" + metadataClassName + "_" + propertyName;
    }


    @PostConstruct
    public void init() {
        CouchbaseEnvironment env = DefaultCouchbaseEnvironment.builder()
//                .queryEnabled(true) //that's the important part
                .kvTimeout(100000)
                .socketConnectTimeout(100000)
                .build();

        cluster = CouchbaseCluster.create(env, getCouchbaseServerIp());

    }

    public void setBucketPassword(String bucketPassword) {
        this.bucketPassword = bucketPassword;
    }

    public String getBucketPassword() {
        return bucketPassword;
    }
}
