package org.metaworks.springboot.configuration;

import org.metaworks.common.Metaworks4RemoteServiceImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

@SpringBootApplication
public abstract class Metaworks4BaseApplication extends JpaBaseConfiguration implements ApplicationContextAware {

	/**
	 * @param dataSource
	 * @param properties
	 * @param jtaTransactionManagerProvider
	 */
	protected Metaworks4BaseApplication(DataSource dataSource, JpaProperties properties,
                                        ObjectProvider<JtaTransactionManager> jtaTransactionManagerProvider,
                                        ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		super(dataSource, properties, jtaTransactionManagerProvider, transactionManagerCustomizers);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration#createJpaVendorAdapter()
	 */
	@Override
	protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
		return new EclipseLinkJpaVendorAdapter();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration#getVendorProperties()
	 */
	@Override
	protected Map<String, Object> getVendorProperties() {

		// Turn off dynamic weaving to disable LTW lookup in static weaving mode
		return Collections.singletonMap("eclipselink.weaving", (Object)"false");
	}

	static ApplicationContext applicationContext;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

			this.applicationContext = applicationContext;

			new Metaworks4RemoteServiceImpl(); // register Metaworks4RemoteServiceImpl as default instance

		}

		public static ApplicationContext getApplicationContext() {
			return applicationContext;
		}


}