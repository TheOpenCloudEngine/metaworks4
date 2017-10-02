package org.metaworks.common;

import org.metaworks.dwr.MetaworksRemoteService;
import org.metaworks.springboot.configuration.Metaworks4BaseApplication;
import org.springframework.context.ApplicationContext;

/**
 * Created by uengine on 2017. 10. 2..
 */
public class Metaworks4RemoteServiceImpl extends MetaworksRemoteService {
    public Metaworks4RemoteServiceImpl() {
        setInstance(this);
    }

    @Override
    public ApplicationContext getBeanFactory() {
        return Metaworks4BaseApplication.getApplicationContext();
    }
}
