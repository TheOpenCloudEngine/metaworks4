package org.metaworks.multitenancy.persistence;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.metaworks.ObjectInstance;
import org.metaworks.WebFieldDescriptor;
import org.metaworks.WebObjectType;
import org.metaworks.annotation.RestAssociation;
import org.metaworks.dwr.MetaworksRemoteService;
import org.oce.garuda.multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.uengine.util.UEngineUtil;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static javafx.scene.input.KeyCode.T;

@Transactional
public class MultitenantRepositoryImpl<E, PK extends Serializable> extends
        SimpleJpaRepository<E, PK> implements MultitenantRepository<E, PK> {

    private final EntityManager entityManager;
    private final JpaEntityInformation<E, ?> entityInformation;

    public MultitenantRepositoryImpl(final JpaEntityInformation<E, ?> entityInformation,
                                     final EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
        getEntityManager().setProperty("tenant-id", TenantContext.getThreadLocalInstance().getTenantId());
    }

    @Override
    @Transactional
    public void delete(final E entity) {
        getEntityManager().setProperty("tenant-id", TenantContext.getThreadLocalInstance().getTenantId());
        super.delete(entity);
    }

    @Override
    public List<E> findAll() {
        getEntityManager().setProperty("tenant-id", TenantContext.getThreadLocalInstance().getTenantId());
        return super.findAll();
    }

    @Override
    public Optional<E> findOne(Specification<E> spec) {
        getEntityManager().setProperty("tenant-id", TenantContext.getThreadLocalInstance().getTenantId());
        Optional<E> one = super.findOne(spec);
        if (!Optional.empty().equals(one)) {
            E s = one.get();
            if (s != null && s instanceof AfterLoadOne) {
                ((AfterLoadOne) s).afterLoadOne();
            }
        }
        return one;
    }

    @Override
    public Optional<E> findById(final PK pk) {
        getEntityManager().setProperty("tenant-id", TenantContext.getThreadLocalInstance().getTenantId());
        Optional<E> one = super.findById(pk);
        if (!Optional.empty().equals(one)) {
            E s = one.get();
            if (s != null && s instanceof AfterLoadOne) {
                ((AfterLoadOne) s).afterLoadOne();
            }
        }
        return one;
    }

    private Specification<E> isMyTenantData(final String tenantId) {
        return new Specification<E>() {

            @Override
            public Predicate toPredicate(Root<E> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                final Predicate tenant = cb.equal(root.get("tenantID"), tenantId);
                //final Predicate hidden = cb.isFalse(root.<Boolean> get("deleted"));
                return tenant;//cb.and(id, hidden);
            }

        };
    }

    @Autowired
    private ApplicationContext context;

    public void beforeSave(MultitenantEntity multitenantEntity) {
        System.out.println();
    }

    public void afterLoad(MultitenantEntity multitenantEntity) {
    }

    public String _key(MultitenantEntity multitenantEntity) {
        WebObjectType type = null;
        try {
            String className = multitenantEntity.getClass().getName();
            type = MetaworksRemoteService.getInstance().getMetaworksType(className);

            org.metaworks.Type m2type = type.metaworks2Type();
            ObjectInstance instance = (ObjectInstance) m2type.createInstance();
            instance.setObject(multitenantEntity);
            Object keyFieldValue = instance.getFieldValue(type.getKeyFieldDescriptor().getDisplayName());

            return (className + "@" + keyFieldValue);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <S extends E> S save(S entity) {
        getEntityManager().setProperty("tenant-id", TenantContext.getThreadLocalInstance().getTenantId());

        if (entity instanceof BeforeSave) {
            ((BeforeSave) entity).beforeSave();
        }

        if (entity instanceof MultitenantEntity) {
            beforeSave((MultitenantEntity) entity);
        }

        if (true || entity.getClass().isAnnotationPresent(RestAssociation.class)) {
            Field[] fields = entity.getClass().getDeclaredFields();

            for (Field field : fields) {

                RestAssociation restAssociation = field.getAnnotation(RestAssociation.class);

                if (restAssociation != null) {
                    String joinColumnName = restAssociation.joinColumn();

                    if (UEngineUtil.isNotEmpty(joinColumnName))
                        try {
                            Object joinFieldValue = null;

                            try {
                                joinFieldValue = field.get(entity);
                            } catch (Exception ex) {
                            }

                            if (joinFieldValue == null) {
                                try {
                                    joinFieldValue = entity.getClass().getMethod("get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1)).invoke(entity, new Object[]{});
                                } catch (Exception ex) {
                                }
                            }

                            if (joinFieldValue != null) {

                                WebObjectType webObjectType = MetaworksRemoteService.getInstance().getMetaworksType(field.getType().getName());
                                WebFieldDescriptor fieldDescriptor = webObjectType.getKeyFieldDescriptor();
                                String joinFieldPrimaryKey = fieldDescriptor.getName();

                                Object joinFieldIdValue = joinFieldValue.getClass()
                                        .getMethod("get" + joinFieldPrimaryKey.substring(0, 1).toUpperCase() + joinFieldPrimaryKey.substring(1))
                                        .invoke(joinFieldValue);

                                if (joinFieldIdValue != null) {
                                    entity.getClass()
                                            .getMethod("set" + joinColumnName.substring(0, 1).toUpperCase() + joinColumnName.substring(1), new Class[]{joinFieldIdValue.getClass()})
                                            .invoke(entity, new Object[]{joinFieldIdValue});
                                }
                            }

//                    } catch (IllegalAccessException e) {
//                        throw new RuntimeException("Failed to get id value from rest join column: " + entity.getClass() +"."+ field.getName(), e);
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException("Failed to set @RestAssociation data: Be sure a setter method for '" + restAssociation.joinColumn() + "' (with same parameter type with the foreign key) present.", e);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to get id value from rest join column: " + entity.getClass() + "." + field.getName(), e);
                        }
                }
            }
        }

        return super.save(entity);
    }

    @Override
    public Page<E> findAll(Pageable pageable) {

        getEntityManager().setProperty("tenant-id", TenantContext.getThreadLocalInstance().getTenantId());

        Page<E> page = super.findAll(pageable);

        for (E entity : page.getContent()) {

            if (entity instanceof AfterLoad) {
                ((AfterLoad) entity).afterLoad();
            }

//            if(entity instanceof AfterLoadOne) {
//                ((AfterLoadOne) entity).afterLoadOne();
//            }

            if (entity instanceof MultitenantEntity) {
                afterLoad((MultitenantEntity) entity);
            }

        }

        return page;
    }

    @Override
    public List<E> findAllById(Iterable<PK> pks) {
        return super.findAllById(pks);
    }

    @Override
    public List<E> findAll(Sort sort) {
        return super.findAll(sort);
    }

    @Override
    public List<E> findAll(Specification<E> spec) {
        return super.findAll(spec);
    }

    @Override
    public Page<E> findAll(Specification<E> spec, Pageable pageable) {
        return super.findAll(spec, pageable);
    }

    @Override
    public List<E> findAll(Specification<E> spec, Sort sort) {
        return super.findAll(spec, sort);
    }

    public EntityManager getEntityManager() {
        return this.entityManager;
    }

    protected JpaEntityInformation<E, ?> getEntityInformation() {
        return this.entityInformation;
    }
}