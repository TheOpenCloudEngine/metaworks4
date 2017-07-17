package org.metaworks.springboot.configuration;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.metaworks.annotation.RestAggregator;
import org.metaworks.multitenancy.ClassManager;
import org.metaworks.multitenancy.CouchbaseMetadataService;
import org.metaworks.multitenancy.MetadataService;
import org.metaworks.multitenancy.tenantawarefilter.TenantAwareFilter;
import org.metaworks.multitenancy.persistence.MultitenantRepositoryImpl;
import org.metaworks.rest.MetaworksRestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.hateoas.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.uengine.modeling.resource.CachedResourceManager;
import org.uengine.modeling.resource.ResourceManager;
import org.uengine.modeling.resource.Storage;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@EnableWebMvc
@ComponentScan(basePackageClasses = {TenantAwareFilter.class, MetaworksRestService.class, ClassManager.class, MetadataService.class})
@EnableJpaRepositories(repositoryBaseClass = MultitenantRepositoryImpl.class)
public abstract class Metaworks4WebConfig extends WebMvcConfigurerAdapter {
//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        converters.add(new GsonHttpMessageConverter());
//    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("POST", "GET", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("access_token", "Content-Type");

//
//        registry.addMapping("/**").allowedOrigins("*");
//        registry.addMapping("/people").allowedOrigins("*");
//        registry.addMapping("/**").allowedOrigins("http://localhost:8081");
//        registry.addMapping("/people").allowedOrigins("http://localhost:8081");
    }

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
        CouchbaseMetadataService metadataService = new CouchbaseMetadataService();
        metadataService.setCouchbaseServerIp("localhost");
        metadataService.setBucketName("default");

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

        return propertiesMap;
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

    @Autowired
    EntityLinks entityLinks;

    @Bean
    public ResourceProcessor<Resource<?>> resourceProcessor() {
        return new ResourceProcessor<Resource<?>>() {
            @Override
            public Resource<?> process(Resource<?> resource) {
                // additional processing only for entities that have rest resources
                if (resource.getContent().getClass().isAnnotationPresent(RestAggregator.class)) {
                    Map<String, String> links = new HashMap<String, String>();

                    // process any fields that have the RestResourceMapper annotation
                    Field[] fields = resource.getContent().getClass().getDeclaredFields();

                    for(Field field : fields){

                        RestAggregator restResourceMapper = field.getAnnotation(RestAggregator.class);

                        if (restResourceMapper!=null && resource.getId()!=null) {
                            String resourceId = resource.getId().getRel();

                            if (resourceId!=null) {
                                // construct a REST endpoint URL from the annotation properties and resource id
                                final String restResourceURL = "/"+restResourceMapper.path() + "/" + resource.getContent().getClass().getSimpleName().toLowerCase() + "/" + resourceId;

                                try {
                                    URL resourceURL = new URL(restResourceURL);
                                    Class entityClass = resource.getContent().getClass();
                                    //use HATEOAS LinkBuilder to get the right host and port for constructing the appropriate resource link
                                    LinkBuilder linkBuilder = entityLinks.linkFor(entityClass);
                                    URL hateoasURL = new URL(linkBuilder.withSelfRel().getHref());

                                    resourceURL = new URL(
                                            "${hateoasURL.protocol}://${hateoasURL.host}:${hateoasURL.port}${resourceURL.path}"
                                    );

                                    links.put(field.getName(), resourceURL.toString());
                                }catch (Exception e) {
                                }
                            }

                        }
                        // add any additional links to the output
                        for(String linkResourceName : links.values()){
                            resource.add(new Link(links.get(linkResourceName), linkResourceName));
                        }

                    }

                }

                return resource;
            }
        };
    }

}