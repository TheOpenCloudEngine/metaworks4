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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.spi.EvaluationContextExtension;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.hateoas.*;
import org.springframework.hateoas.core.EmbeddedWrapper;
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

    @Autowired
    private LoadBalancerClient loadBalancer;


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

                        RestAssociation restAssociation = field.getAnnotation(RestAssociation.class);

                        if (restAssociation != null && resource.getId() != null) {
                            String resourceId = resource.getId().getRel();

                            if (resourceId != null) {
                                // construct a REST endpoint URL from the annotation properties and entity id
                                String path = restAssociation.path();


//                                path = path.replaceAll("\\{\\{entity.name\\}\\}", entity.getContent().getClass().getSimpleName().toLowerCase());
//                                path = path.replaceAll("\\{\\{tenantId\\}\\}", TenantContext.getThreadLocalInstance().getTenantId());
//                                path = path.replaceAll("\\{\\{\\@id\\}\\}", entity.getContent().toString());


                                path = evaluatePath(path, resource.getContent());

                                if(!path.startsWith("/")) path = "/" + path;

                                try {
                                    URL resourceURL;
                                    Class entityClass = resource.getContent().getClass();
                                    //use HATEOAS LinkBuilder to get the right host and port for constructing the appropriate entity link

                                    if ("self".equals(restAssociation.serviceId())) {

                                        EntityLinks entityLinks = MetaworksRemoteService.getInstance().getComponent(EntityLinks.class);

                                        LinkBuilder linkBuilder = entityLinks.linkFor(entityClass);
                                        URL selfURL = new URL(linkBuilder.withSelfRel().getHref());

                                        resourceURL = new URL(
                                                selfURL.getProtocol() + "://" + selfURL.getHost() + ":" + selfURL.getPort() + path
                                        );
                                    }else
                                    if (restAssociation.serviceId().startsWith("http")) {
                                        resourceURL = new URL(
                                                restAssociation.serviceId() + path
                                        );
                                    } else { //find by serviceId name from the eureka!

                                        ServiceInstance serviceInstance=loadBalancer.choose(restAssociation.serviceId());

                                        if(serviceInstance==null) throw new Exception("Service for service Id "+ restAssociation.serviceId() + " is not found from Loadbalancer (Ribbon tried from Eureka).");

                                        String baseUrl=serviceInstance.getUri().toString();

                                        resourceURL = new URL(
                                                baseUrl + path
                                        );
                                    }

                                    links.put(field.getName(), resourceURL.toString());
                                } catch (Exception e) {
                                    throw new RuntimeException("Error when to add @RestAssociation link", e);
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

    static final String starter = "{";
    static final String ending = "}";

    public String evaluatePath(String expression, Object entity){

        try {

            SpelExpressionParser expressionParser = new SpelExpressionParser();

            int pos;
            int oldpos = 0;
            int endpos;
            String key;
            StringBuffer generating = new StringBuffer();

            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setRootObject(entity);
            context.setVariable("tenant", TenantContext.getThreadLocalInstance());


            while ((pos = expression.indexOf(starter, oldpos)) > -1) {
                pos += starter.length();
                endpos = expression.indexOf(ending, pos);

                if (endpos > pos) {
                    generating.append(expression.substring(oldpos, pos - starter.length()));
                    key = expression.substring(pos, endpos);

                    key = key.trim();

                    Object val = expressionParser.parseExpression(key).getValue(context);

                    if (val != null)
                        generating.append("" + val);
                }
                oldpos = endpos + ending.length();
            }

            return generating.toString();
        }catch(Exception e){
            throw new RuntimeException("Error to parse expression " + expression, e);
        }


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