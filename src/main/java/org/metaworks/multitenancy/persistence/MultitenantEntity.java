package org.metaworks.multitenancy.persistence;

import lombok.Data;
import org.eclipse.persistence.annotations.Multitenant;
import org.eclipse.persistence.annotations.TenantDiscriminatorColumn;
import org.metaworks.annotation.Hidden;
import org.metaworks.annotation.RestAssociation;
import org.springframework.stereotype.Component;

import javax.persistence.Column;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * Created by uengine on 2017. 7. 6..
 */
@Component
@Multitenant
@TenantDiscriminatorColumn(name = "TENANTID", contextProperty = "tenant-id")
public class MultitenantEntity implements Serializable {


    @Column(name = "TENANTID", insertable = false, updatable = false)
    String tenantId;

    @Hidden
    public String getTenantId() {
        return tenantId;
    }

    @Transient
    @RestAssociation(
            path = "/metadata/{{entity.name}}/{{@id}}", serviceId = "mongodb"
    )
    TenantProperties tenantProperties;

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public TenantProperties getTenantProperties() {
        return tenantProperties;
    }

    public void setTenantProperties(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }
}
