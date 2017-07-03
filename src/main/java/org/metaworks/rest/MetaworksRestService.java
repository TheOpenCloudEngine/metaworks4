package org.metaworks.rest;

import org.boon.Boon;
import org.directwebremoting.ServerContextFactory;
import org.metaworks.*;
import org.metaworks.common.MetaworksUtil;
import org.metaworks.dao.IDAO;
import org.metaworks.dao.TransactionContext;
import org.metaworks.dwr.MetaworksRemoteService;
import org.metaworks.dwr.TransactionalDwrServlet;
import org.metaworks.multitenancy.ClassManager;
import org.metaworks.multitenancy.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.uengine.uml.model.ClassDefinition;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by uengine on 2017. 5. 21..
 */
@RestController
public class MetaworksRestService{

    @PostConstruct
    public void init(){
        System.out.println();
    }

   // @CrossOrigin(origins = "*")
    @RequestMapping("/metadata")
    public WebObjectType getMetadata(@RequestParam(value="className", defaultValue="") String className) throws Exception {
//
//        InputStream inputStream = req.getInputStream();
//        ByteArrayOutputStream bao = new ByteArrayOutputStream();
//        try {
//            MetaworksUtil.copyStream(inputStream, bao);
//            Object inputObject = Boon.fromJson(bao.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return MetaworksRemoteService.getInstance().getMetaworksType(className);
    }

    @Autowired
    private ApplicationContext applicationContext;

    private ApplicationContext getBeanFactory(){
        return applicationContext;
    }

    @RequestMapping(value = "/rpc", method = RequestMethod.POST)
    public Object callMetaworksService(@RequestBody InvocationContext invocationContext) throws Throwable{


        String objectTypeName = invocationContext.getObjectTypeName();
        Object clientObject = invocationContext.getClientObject();
        String methodName = invocationContext.getMethodName();
        Map<String, Object> autowiredFields = invocationContext.getAutowiredFields();

        Class serviceClass = Thread.currentThread().getContextClassLoader().loadClass(objectTypeName);


        //if the requested value object is IDAO which need to be converted to implemented one so that it can be invoked by its methods
        //Another case this required is when Spring is used since the spring base object should be auto-wiring operation
        ApplicationContext springAppContext = null;
        if(TransactionalDwrServlet.useSpring) springAppContext = getBeanFactory();
        Object springBean = null;
        if(springAppContext!=null)
            try{
                //springBean = getBeanFactory().getBean(serviceClass);
                Map beanMap = springAppContext.getBeansOfType(serviceClass);
                Set keys = beanMap.keySet();
                for (Object key : keys) {
                    if(springBean != null) {
                        System.err.println("====== Warnning : MetaworksRemoteService.callMetaworksService get only one bean object of one class.");
                        break;
                    }
                    springBean = beanMap.get(key);
                }
            }catch(Exception e){
                //TODO: check if there's any occurrence of @Autowired in the field list, it is required to check and shows some WARNING to the developer.
            }

        //firing ConverterEventListener.afterConverted()
        //fireAfterConvertertedEvent(clientObject);  //disabled due to performance issue


        if(serviceClass.isInterface() || springBean!=null){
            String serviceClassNameOnly = WebObjectType.getClassNameOnly(serviceClass);

            if(serviceClass.isInterface()){
                serviceClassNameOnly = serviceClassNameOnly.substring(1, serviceClassNameOnly.length());
                serviceClass = Thread.currentThread().getContextClassLoader().loadClass(serviceClass.getPackage().getName() + "." + serviceClassNameOnly);
            }

            if(springAppContext!=null)
                try{
                    springBean = getBeanFactory().getBean(serviceClass);
                }catch(Exception e){
                    //TODO: check if there's any occurrence of @Autowired in the field list, it is required to check and shows some WARNING to the developer.
                }


            WebObjectType wot = MetaworksRemoteService.getInstance().getMetaworksType(serviceClass.getName());

            //Convert to service object from value object (IDAO)
            ObjectInstance srcInst = (ObjectInstance) wot.metaworks2Type().createInstance();
            srcInst.setObject(clientObject);
            ObjectInstance targetInst = (ObjectInstance) wot.metaworks2Type().createInstance();

            if(springBean!=null){
                targetInst.setObject(springBean);
            }

            for(FieldDescriptor fd : wot.metaworks2Type().getFieldDescriptors()){
                Object srcFieldValue = srcInst.getFieldValue(fd.getName());

                //MetaworksObject need to initialize the property MetaworksContext if there's no data.
                if("MetaworksContext".equals(fd.getName()) && srcFieldValue==null && IDAO.class.isAssignableFrom(serviceClass)){
                    srcFieldValue = new MetaworksContext();
                }

                boolean isSpringAutowiredField = false;
                try{
                    isSpringAutowiredField = ((serviceClass.getMethod( "get"+ fd.getName(), new Class[]{})).getAnnotation(Autowired.class) != null);
                }catch(Exception e){
                }

                if(!isSpringAutowiredField)
                    targetInst.setFieldValue(fd.getName(), srcFieldValue);
            }

            clientObject = targetInst.getObject();

        }

        //injecting autowired fields from client
        if(autowiredFields!=null){
            for(String fieldName : autowiredFields.keySet()){
                Object autowiringValue = autowiredFields.get(fieldName);

                if(!fieldName.startsWith(ServiceMethodContext.WIRE_PARAM_CLS)) //if the autowired field is not a @AutowiredFromClient in Parameterized call, means normal case in the field injection.
                    serviceClass.getField(fieldName).set(clientObject, autowiringValue);
            }
        }

        Map autowiredObjects = MetaworksRemoteService.getInstance().autowire(clientObject, false);

        Method theMethod = null;
        boolean parameterizedInvoke = false;
        ServiceMethodContext theSMC = null;

        WebObjectType wot = MetaworksRemoteService.getInstance().getMetaworksType(serviceClass.getName());
        for(ServiceMethodContext smc: wot.getServiceMethodContexts()){ //TODO: [Performance] looking in array
            if(smc.getMethodName().equals(methodName)){
                theSMC = smc;
                theMethod = smc._getMethod();
                parameterizedInvoke = theSMC._getPayloadParameterIndexes()!=null && theSMC._getPayloadParameterIndexes().size() > 0;
            }
        }



//		object = invocationContext.getObject();

        ///put autowiring objects all including the object from client itself.
        TransactionContext.getThreadLocalInstance().setAutowiringObjectsFromClient(autowiredFields);
        if(TransactionContext.getThreadLocalInstance().getAutowiringObjectsFromClient()==null){
            TransactionContext.getThreadLocalInstance().setAutowiringObjectsFromClient(new HashMap());
        }
        TransactionContext.getThreadLocalInstance().getAutowiringObjectsFromClient().put("this", clientObject);

        //if we failed to find method by class name, just try to get the method from object directly.
        if(theMethod == null && clientObject!=null)
            theMethod = clientObject.getClass().getMethod(methodName, new Class[]{});

        try{

            Object[] parameters = new Object[theMethod.getParameterTypes().length];
            if(parameterizedInvoke){

                for(String key : theSMC._getPayloadParameterIndexes().keySet()){
                    int parameterIndex = theSMC._getPayloadParameterIndexes().get(key);

                    Object fieldValue = null;
                    if(key.startsWith(ServiceMethodContext.WIRE_PARAM_CLS)){
                        fieldValue = autowiredFields.get(key);
                    }else{
                        try{
                            fieldValue = serviceClass.getMethod("get" + key.substring(0, 1).toUpperCase() + key.substring(1), new Class[]{}).invoke(clientObject, new Object[]{});
                        }catch(Exception ex){
                            throw new RuntimeException("Error when to get field ["+ key + "] value for calling parameterized metaworks call: @Payload('" + key + "')", ex);
                        }
                    }

                    parameters[parameterIndex] = fieldValue;

                }

            }

            Object returned = theMethod.invoke(clientObject, parameters);

            if(theMethod.getReturnType()==void.class) //if void is return type, apply clientobject back to the browser.
                returned = clientObject;


            Object wrappedReturn = TransactionContext.getThreadLocalInstance().getSharedContext("wrappedReturn");
            if(wrappedReturn!=null)
                returned = wrappedReturn;

// moved to MetaworksConverter
//			if(returned instanceof SerializationSensitive){
//				((SerializationSensitive) returned).beforeSerialization();
//			}

            return returned;

        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            if(!(e.getTargetException() instanceof MetaworksException))
                e.printStackTrace();

            throw e.getTargetException();
        }
    }

    @Autowired
    ClassManager classManager;

    @RequestMapping(value = "/classdefinition", method = RequestMethod.POST)
    public void putClassDefinition(@RequestBody ClassDefinition classDefinition) throws Exception{
        classManager.setClassName(classDefinition.getName());
        classManager.setClassDefinition(classDefinition);
        classManager.save();

    }


    @RequestMapping(value = "/classdefinition", method = RequestMethod.GET)
    public ClassDefinition getClassDefinition(@RequestParam(value="className", defaultValue="") String className) throws Exception{
        classManager.setClassName(className);
        classManager.load();
        return classManager.getClassDefinition();
    }

}
