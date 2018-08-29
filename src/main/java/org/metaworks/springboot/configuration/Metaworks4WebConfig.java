package org.metaworks.springboot.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.metaworks.annotation.AddMetadataLink;
import org.metaworks.annotation.RestAssociation;
import org.metaworks.common.ApplicationContextRegistry;
import org.metaworks.dwr.MetaworksRemoteService;
import org.metaworks.iam.SecurityEvaluationContextExtension;
import org.metaworks.multitenancy.ClassManager;
import org.metaworks.multitenancy.DefaultMetadataService;
import org.metaworks.multitenancy.MetadataService;
import org.metaworks.multitenancy.persistence.AfterLoadOne;
import org.metaworks.multitenancy.persistence.MultitenantRepositoryImpl;
import org.metaworks.rest.MetaworksRestService;
import org.oce.garuda.multitenancy.TenantContext;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.query.spi.EvaluationContextExtension;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.UriToEntityConverter;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.json.PersistentEntityJackson2Module;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.hateoas.*;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.uengine.modeling.resource.CachedResourceManager;
import org.uengine.modeling.resource.ResourceManager;
import org.uengine.modeling.resource.Storage;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@EnableWebMvc
@ComponentScan(basePackageClasses = {/*TenantAwareFilter.class,*/ MetaworksRestService.class, ClassManager.class, MetadataService.class})
@EnableJpaRepositories(repositoryBaseClass = MultitenantRepositoryImpl.class)
public abstract class Metaworks4WebConfig extends RepositoryRestMvcConfiguration {// RepositoryRestConfigurerAdapter {

//    public static ObjectMapper jpaMapper;

//    @Override
//    public void configureJacksonObjectMapper(ObjectMapper objectMapper) {
//        //jpaMapper = objectMapper.registerModule(new JavaTimeModule());
//    }

    public Metaworks4WebConfig(ApplicationContext context, ObjectFactory<ConversionService> conversionService) {
        super(context, conversionService);
    }

    protected Module persistentEntityJackson2Module() {
        PersistentEntities entities = this.persistentEntities();
        DefaultFormattingConversionService conversionService = this.defaultConversionService();
        UriToEntityConverter uriToEntityConverter = this.uriToEntityConverter(conversionService);
        RepositoryInvokerFactory repositoryInvokerFactory = this.repositoryInvokerFactory(conversionService);
        PersistentEntityJackson2Module module = (PersistentEntityJackson2Module) super.persistentEntityJackson2Module();
        module.setDeserializerModifier(new AssociationUriResolvingDeserializerModifier_(entities, this.associationLinks(), uriToEntityConverter, repositoryInvokerFactory));
        return module;
    }


    public class AssociationUriResolvingDeserializerModifier_ extends PersistentEntityJackson2Module.AssociationUriResolvingDeserializerModifier {
        PersistentEntities entities;

        public AssociationUriResolvingDeserializerModifier_(PersistentEntities entities, Associations associationLinks, UriToEntityConverter converter, RepositoryInvokerFactory factory) {
            super(entities, associationLinks, converter, factory);
            this.entities = entities;
        }

        public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {

            builder = super.updateBuilder(config, beanDesc, builder);

            Iterator properties = builder.getProperties();
            PersistentEntity entity = this.entities.getPersistentEntity(beanDesc.getBeanClass()).get();

            if (entity != null) {
                PersistentProperty idProperty = entity.getIdProperty();

                while (properties.hasNext()) {
                    SettableBeanProperty property = (SettableBeanProperty) properties.next();
                    //PersistentProperty persistentProperty = entity.getPersistentProperty(property.getName());
                    JsonDeserializer deserializer;

                    if (property.getAnnotation(RestAssociation.class) != null) {
                        deserializer = new RestAssociationUriStringDeserializer(property, entities);//, beanDesc, idProperty);
                        builder.addOrReplaceProperty(property.withValueDeserializer(deserializer), false);
                    }
                }
                return builder;
            }
            return builder;
        }
    }

    public class RestAssociationUriStringDeserializer extends StdDeserializer<Object> {
        private static final long serialVersionUID = -2175900204153350125L;
        private static final String UNEXPECTED_VALUE = "Expected URI cause property %s points to the managed domain type!";
        private final SettableBeanProperty property;
        PersistentEntities entities;
//        private final BeanDescription beanDescription;
//        PersistentProperty idProperty;

        public RestAssociationUriStringDeserializer(SettableBeanProperty property, PersistentEntities entities) {//, BeanDescription beanDescription, PersistentProperty idProperty) {
            super(property.getType());
            this.property = property;
//            this.beanDescription = beanDescription;
//            this.idProperty = idProperty;

            this.entities = entities;
        }

        public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String source = jp.getValueAsString();
            if (!StringUtils.hasText(source)) {
                return null;
            } else {
                try {
                    URI uri = (new UriTemplate(source)).expand(new Object[0]);

                    String[] pathElements = uri.getPath().split("/");
                    String lastPathElement = pathElements[pathElements.length - 1];

                    PersistentEntity entity = this.entities.getPersistentEntity(this.property.getType().getRawClass()).get();

                    PersistentProperty idProperty = entity.getIdProperty();

                    Object idValue = null;
                    if (String.class.equals(idProperty.getType())) {
                        idValue = lastPathElement.trim();
                    } else if (Long.class.equals(idProperty.getType())) {
                        idValue = Long.valueOf(lastPathElement);
                    }

                    if (idValue != null) {
                        Object bean = property.getType().getRawClass().newInstance();
                        idProperty.getSetter().invoke(bean, new Object[]{idValue});

                        return bean;
                    } else {
                        return null;
                    }
                    //return this.converter.convert(o_O, PersistentEntityJackson2Module.URI_DESCRIPTOR, typeDescriptor);
                } catch (IllegalArgumentException var6) {
                    throw ctxt.weirdStringException(source, URI.class, String.format("Expected URI cause property %s points to the managed domain type!", new Object[]{this.property}));
                } catch (IllegalAccessException e) {
                    throw ctxt.weirdStringException(source, URI.class, String.format("URI for property %s can't converted to domain type! detail: %s", new Object[]{this.property, e.getMessage()}));
                } catch (InvocationTargetException e) {
                    throw ctxt.weirdStringException(source, URI.class, String.format("URI for property %s can't converted to domain type!", new Object[]{this.property}));
                } catch (InstantiationException e) {
                    throw ctxt.weirdStringException(source, URI.class, String.format("URI for property %s can't converted to domain type!", new Object[]{this.property}));

                }
            }
        }

        public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
            return this.deserialize(jp, ctxt);
        }
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
        DefaultMetadataService metadataService = new DefaultMetadataService();
        metadataService.setResourceManager(resourceManager());

        return metadataService;
    }

    @Bean
    @Primary
    public JpaProperties jpaProperties() {

        JpaProperties propertiesMap = new JpaProperties();
        propertiesMap.getProperties().put(PersistenceUnitProperties.DDL_GENERATION, PersistenceUnitProperties.CREATE_OR_EXTEND);
        propertiesMap.getProperties().put(PersistenceUnitProperties.LOGGING_LEVEL, "FINE");
        propertiesMap.getProperties().put("hibernate.hbm2ddl.auto", "create");

        return propertiesMap;
    }


    @Bean
    EvaluationContextExtension securityExtension() {
        return new SecurityEvaluationContextExtension();
    }


    @Bean
    public ResourceProcessor<Resources<Resource<?>>> resourceProcessorForAddingMetadata() {
        return new MetadataResourceProcessor();
    }

    private class MetadataResourceProcessor implements ResourceProcessor<Resources<Resource<?>>> {
        @Override
        public Resources<Resource<?>> process(Resources<Resource<?>> resources) {

            Object contentElem = ((java.util.Collection) resources.getContent()).iterator().next();

            Class entityType;

            if (contentElem instanceof EmbeddedWrapper) {
                entityType = ((EmbeddedWrapper) contentElem).getRelTargetType();

            } else {
                entityType = ((PersistentEntityResource) contentElem).getContent().getClass();
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

                    //TODO: why are you live in here?
                    if (resource.getContent() instanceof AfterLoadOne) {
                        ((AfterLoadOne) resource.getContent()).afterLoadOne();
                    }

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

                                if (path == null) continue;

                                if (!path.startsWith("/")) path = "/" + path;

                                try {
                                    URL resourceURL;
                                    Class entityClass = resource.getContent().getClass();
                                    //use HATEOAS LinkBuilder to get the right host and port for constructing the appropriate entity link

                                    if ("self".equals(restAssociation.serviceId())) {

                                        EntityLinks entityLinks = MetaworksRemoteService.getInstance().getComponent(EntityLinks.class);

                                        LinkBuilder linkBuilder = entityLinks.linkFor(entityClass);
                                        URL selfURL = new URL(linkBuilder.withSelfRel().getHref());

                                        resourceURL = new URL(
                                                selfURL.getProtocol() + "://" + selfURL.getHost() + (selfURL.getPort() > -1 ? ":" + selfURL.getPort() : "") + path
                                        );
                                    } else if (restAssociation.serviceId().startsWith("http")) {
                                        resourceURL = new URL(
                                                restAssociation.serviceId() + path
                                        );
                                    } else { //find by serviceId name from the eureka!

                                        ServiceInstance serviceInstance = loadBalancer.choose(restAssociation.serviceId());

                                        if (serviceInstance == null)
                                            throw new Exception("Service for service Id " + restAssociation.serviceId() + " is not found from Loadbalancer (Ribbon tried from Eureka).");

                                        String baseUrl = serviceInstance.getUri().toString();

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

                    }

                    // add any additional links to the output
                    for (String linkResourceName : links.keySet()) {
                        resource.add(new Link(links.get(linkResourceName), linkResourceName));
                    }

                }

                return resource;
            }
        };
    }

    static final String starter = "{";
    static final String ending = "}";

    public String evaluatePath(String expression, Object entity) {

        boolean allIsNull = true;
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


            //TODO: change to a major template engine such as Grunt which supports SpEL
            while ((pos = expression.indexOf(starter, oldpos)) > -1) {
                pos += starter.length();
                endpos = expression.indexOf(ending, pos);

                if (endpos > pos) {
                    generating.append(expression.substring(oldpos, pos - starter.length()));
                    key = expression.substring(pos, endpos);

                    key = key.trim();

                    Object val = null;

                    try {
                        val = expressionParser.parseExpression(key).getValue(context);
                        allIsNull = false;
                    } catch (Exception e) {
                        throw e;
                    }

                    if (val != null)
                        generating.append("" + val);
                }
                oldpos = endpos + ending.length();
            }

            generating.append(expression.substring(oldpos));

            return allIsNull ? null : generating.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error to parse expression " + expression, e);
        }


    }

    @Bean
    public ApplicationContextRegistry applicationContextRegistry() {
        return new ApplicationContextRegistry();
    }

}