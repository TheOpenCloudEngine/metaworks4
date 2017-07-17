package org.metaworks.multitenancy.persistence;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.eclipse.persistence.annotations.Multitenant;
import org.eclipse.persistence.annotations.TenantDiscriminatorColumn;
import org.metaworks.ObjectInstance;
import org.metaworks.WebObjectType;
import org.metaworks.annotation.Hidden;
import org.metaworks.annotation.RestAggregator;
import org.metaworks.dwr.MetaworksRemoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.uengine.modeling.resource.DefaultResource;
import org.uengine.modeling.resource.IResource;
import org.uengine.modeling.resource.ResourceManager;

import javax.persistence.Column;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by uengine on 2017. 7. 6..
 */

@Component
@Multitenant
@TenantDiscriminatorColumn(name = "TENANTID", contextProperty = "tenant-id")
public class MultitenantEntity implements Serializable {


    @Column(name="TENANTID", insertable=false, updatable=false)
    String tenantId;
    @Hidden
    public String getTenantId() {
        return tenantId;
    }
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }


    @Transient
    TenantProperties tenantProperties;
    @RestAggregator(
            path="/metadata/{{entity.name}}/{{@id}}", role="mongodb"
    )
        public TenantProperties getTenantProperties() {
            return tenantProperties;
        }
        public void setTenantProperties(TenantProperties tenantProperties) {
            this.tenantProperties = tenantProperties;
        }


//    @Transient
//    Map<String, String> props_;
//    @JsonAnyGetter
//    @Hidden
//    public Map<String, String> getProps_() {
//        return props_;
//    }
//    public void setProps_(Map<String, String> props_) {
//        this.props_ = props_;
//    }
//    @JsonAnySetter
//    public void addProps_(String key, String value) {
//        if(this.props_ == null)
//            this.props_ = new HashMap<String, String>();
//
//        this.props_.put(key, value);
//    }
//
//    @Autowired
//    ResourceManager resourceManager;



}
