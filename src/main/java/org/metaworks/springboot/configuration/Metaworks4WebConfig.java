package org.metaworks.springboot.configuration;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.metaworks.annotation.AddMetadataLink;
import org.metaworks.annotation.RestAssociation;
import org.metaworks.common.ApplicationContextRegistry;
import org.metaworks.dwr.MetaworksRemoteService;
import org.metaworks.iam.SecurityEvaluationContextExtension;
import org.metaworks.multitenancy.ClassManager;
import org.metaworks.multitenancy.DefaultMetadataService;
import org.metaworks.multitenancy.MetadataService;
import org.metaworks.multitenancy.persistence.MultitenantRepositoryImpl;
import org.metaworks.rest.MetaworksRestService;
import org.oce.garuda.multitenancy.TenantContext;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.spi.EvaluationContextExtension;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.*;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.uengine.modeling.resource.CachedResourceManager;
import org.uengine.modeling.resource.ResourceManager;
import org.uengine.modeling.resource.Storage;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@EnableWebMvc
@ComponentScan(basePackageClasses = {/*TenantAwareFilter.class,*/ MetaworksRestService.class, ClassManager.class, MetadataService.class})
@EnableJpaRepositories(repositoryBaseClass = MultitenantRepositoryImpl.class)
public abstract class Metaworks4WebConfig extends WebMvcConfigurerAdapter {
//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        converters.add(new GsonHttpMessageConverter());
//    }

//    @Bean
//    public CorsFilter corsFilter() {
//        return new CorsFilter();
//    }

    @Bean
    public ResourceManager resourceManager() {
        ResourceManager resourceManager = new CachedResourceManager();
        resourceManager.setStorage(storage());
        return resourceManager;
    }

    @Bean
    protected abstract Storage storage();


    @Bean
    public MetadataService metadataService() {
//        CouchbaseMetadataService metadataService = new CouchbaseMetadataService();
//        metadataService.setCouchbaseServerIp("localhost");
//        metadataService.setBucketName("default");

        DefaultMetadataService metadataService = new DefaultMetadataService();
        metadataService.setResourceManager(resourceManager());

        return metadataService;
    }

//    @Bean
//    public DataSource dataSource() {
//        //In classpath from spring-boot-starter-web
//        final Properties pool = new Properties();
//        pool.put("driverClassName", "com.mysql.jdbc.Driver");
//        pool.put("url", "jdbc:mysql://localhost:3306/uengine?useUnicode=true&characterEncoding=UTF8&useOldAliasMetadataBehavior=true");
//        pool.put("username", "root");
//        pool.put("password", "");
//        pool.put("minIdle", 1);
//        try {
//            return new org.apache.tomcat.jdbc.pool.DataSourceFactory().createDataSource(pool);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    @Bean
    @Primary
    public JpaProperties jpaProperties() {

        JpaProperties propertiesMap = new JpaProperties();
        propertiesMap.getProperties().put(PersistenceUnitProperties.DDL_GENERATION, PersistenceUnitProperties.CREATE_OR_EXTEND);
        propertiesMap.getProperties().put(PersistenceUnitProperties.LOGGING_LEVEL, "FINE");
        //propertiesMap.getProperties().put(PersistenceUnitProperties.LOGGING_LEVEL, "FINE");
        //LOGGING_LEVEL

        return propertiesMap;
    }


    @Bean
    EvaluationContextExtension securityExtension() {
        return new SecurityEvaluationContextExtension();
    }


//    @Bean
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final EntityManagerFactoryBuilder builder) {
//        LocalContainerEntityManagerFactoryBean ret = null;
//        try {
//            ret = builder
//                    .dataSource(dataSource())
//                    .packages(Product.class.getPackage().getName())
//                    .persistenceUnit("YourPersistenceUnitName")
//                    .properties(initJpaProperties()).build();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        return ret;
//    }

//    @Autowired
//    EntityLinks entityLinks;

    @Bean
    public ResourceProcessor<Resources<Resource<?>>> resourceProcessorForAddingMetadata() {
        return new MetadataResourceProcessor();
    }

//    @Bean
//    public ResourceProcessor<RepositoryLinksResource> resourceProcessorFor(){
//        return new ResourceProcessor<RepositoryLinksResource>() {
//            @Override
//            public RepositoryLinksResource process(RepositoryLinksResource repositoryLinksResource) {
//                return repositoryLinksResource;
//            }
//        };
//    }


    private class MetadataResourceProcessor implements ResourceProcessor<Resources<Resource<?>>> {
        @Override
        public Resources<Resource<?>> process(Resources<Resource<?>> resources) {

            Object contentElem = ((java.util.Collection)resources.getContent()).iterator().next();

            Class entityType;

            if(contentElem instanceof EmbeddedWrapper){
                entityType = ((EmbeddedWrapper)contentElem).getRelTargetType();

            }else{
                entityType = ((PersistentEntityResource)contentElem).getContent().getClass();
            }


            // if there @AddMetadataLink present, add for metadata service link
            if (entityType.isAnnotationPresent(AddMetadataLink.class)) {
                try {
                    resources.add(linkTo(
                            methodOn(MetaworksRestService.class)
                                    .getClassDefinition(
                                            entityType.getName()
                                    )
                    ).withRel("metadata"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return resources;
        }
    }

    @Bean
    public ResourceProcessor<Resource<?>> resourceProcessorForRestAssociation() {
        return new ResourceProcessor<Resource<?>>() {
            @Override
            public Resource<?> process(Resource<?> resource) {

                // additional processing only for entities that have rest resources
                if (true || resource.getContent().getClass().isAnnotationPresent(RestAssociation.class)) {
                    Map<String, String> links = new HashMap<String, String>();

                    // process any fields that have the RestResourceMapper annotation
                    Field[] fields = resource.getContent().getClass().getDeclaredFields();

                    for (Field field : fields) {

                        RestAssociation restResourceMapper = field.getAnnotation(RestAssociation.class);

                        if (restResourceMapper != null && resource.getId() != null) {
                            String resourceId = resource.getId().getRel();

                            if (resourceId != null) {
                                // construct a REST endpoint URL from the annotation properties and resource id
                                String path = restResourceMapper.path();


                                path = path.replaceAll("\\{\\{entity.name\\}\\}", resource.getContent().getClass().getSimpleName().toLowerCase());
                                path = path.replaceAll("\\{\\{tenantId\\}\\}", TenantContext.getThreadLocalInstance().getTenantId());
                                path = path.replaceAll("\\{\\{\\@id\\}\\}", resource.getContent().toString());

                                try {
                                    URL resourceURL;
                                    Class entityClass = resource.getContent().getClass();
                                    //use HATEOAS LinkBuilder to get the right host and port for constructing the appropriate resource link
                                    //LinkBuilder linkBuilder = entityLinks.linkFor(entityClass);
                                    URL selfURL = new URL("http://localhost");//linkBuilder.withSelfRel().getHref());

                                    if ("self".equals(restResourceMapper.role())) {
                                        resourceURL = new URL(
                                                selfURL.getProtocol() + "://" + selfURL.getHost() + ":" + selfURL.getPort() + path
                                        );
                                    }
                                    if (restResourceMapper.role().startsWith("http")) {
                                        resourceURL = new URL(
                                                restResourceMapper.role() + path
                                        );
                                    } else {
                                        resourceURL = new URL(
                                                selfURL.getProtocol() + "://" + selfURL.getHost() + ":" + selfURL.getPort() + path
                                        );
                                    }

                                    links.put(field.getName(), resourceURL.toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                        // add any additional links to the output
                        for (String linkResourceName : links.keySet()) {
                            resource.add(new Link(links.get(linkResourceName), linkResourceName));
                        }

                    }

                }

                return resource;
            }
        };
    }

    /**
     * 스프링 부트 어플리케이션콘텍스트를 static 으로 사용가능하게 제공.
     *
     * @return ApplicationContextRegistry
     */
    @Bean
    public ApplicationContextRegistry applicationContextRegistry() {
        return new ApplicationContextRegistry();
    }

}