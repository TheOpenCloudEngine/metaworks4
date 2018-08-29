package org.metaworks.multitenancy.persistence;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface MultitenantRepository<E, PK extends Serializable> extends
        JpaRepository<E, PK>, JpaSpecificationExecutor<E> {
}
