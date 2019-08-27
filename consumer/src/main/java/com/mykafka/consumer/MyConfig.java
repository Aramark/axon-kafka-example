/*
 * © 2018 CREALOGIX. All rights reserved.
 */
package com.mykafka.consumer;

import java.util.function.Function;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.Configurer;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.config.EventProcessingModule;
import org.axonframework.eventhandling.EventProcessor;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration;
import org.axonframework.eventhandling.tokenstore.jpa.JpaTokenStore;
import org.axonframework.eventsourcing.eventstore.jdbc.EventSchema;
import org.axonframework.eventsourcing.eventstore.jdbc.EventTableFactory;
import org.axonframework.eventsourcing.eventstore.jdbc.PostgresEventTableFactory;
import org.axonframework.extensions.kafka.eventhandling.DefaultKafkaMessageConverter;
import org.axonframework.extensions.kafka.eventhandling.KafkaMessageConverter;
import org.axonframework.extensions.kafka.eventhandling.consumer.Fetcher;
import org.axonframework.extensions.kafka.eventhandling.consumer.KafkaMessageSource;
import org.axonframework.messaging.StreamableMessageSource;
import org.axonframework.messaging.interceptors.BeanValidationInterceptor;
import org.axonframework.modelling.saga.repository.jdbc.PostgresSagaSqlSchema;
import org.axonframework.modelling.saga.repository.jdbc.SagaSqlSchema;
import org.axonframework.serialization.Serializer;
import org.axonframework.spring.eventsourcing.SpringAggregateSnapshotterFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
This configuration is only needed because of the issue
https://github.com/AxonFramework/AxonFramework/issues/710
https://github.com/AxonFramework/ReferenceGuide/issues/82
 */
@Configuration
//@AutoConfigureAfter(KafkaAutoConfiguration.class)
public class MyConfig {

	@Bean
	public TrackingEventProcessorConfiguration tepConfiguration() {
		System.out.println("Inside tepConfiguration");
		return TrackingEventProcessorConfiguration.forParallelProcessing(4);
	}

	@Bean
	public Configurer configurer(EntityManagerProvider emp, TransactionManager tm) {
		return DefaultConfigurer.defaultConfiguration(true);
		// return DefaultConfigurer.jpaConfiguration(emp, tm);
	}

	@Bean
	public EventProcessor tep(DefaultConfigurer configurer, KafkaMessageSource kms,
			TrackingEventProcessorConfiguration tepConfig) {
//		TrackingEventProcessor.Builder builder = TrackingEventProcessor.builder()
//				.errorHandler(PropagatingErrorHandler.INSTANCE)
//				.eventHandlerInvoker(new MultiEventHandlerInvoker(new SimpleEventHandlerInvoker())).

		EventProcessingModule epm = new EventProcessingModule();
		org.axonframework.config.Configuration configuration = configurer.buildConfiguration();
		epm.initialize(configuration);
		EventProcessingConfigurer epc = epm.registerTrackingEventProcessor("MyProcessor", c -> kms, c -> tepConfig);
		EventProcessor tep = epm.eventProcessors().get("MyProcessor");
		return tep;

//		TrackingEventProcessor.Builder builder = TrackingEventProcessor.builder()
//				.errorHandler(PropagatingErrorHandler.INSTANCE)
//				.trackingEventProcessorConfiguration(tepConfig)
//				.messageSource(kms)
//				.eventHandlerInvoker(new SimpleEventHandlerInvoker())
//				.name("MyProcessor")
//				.rollbackConfiguration(RollbackConfigurationType.RUNTIME_EXCEPTIONS)
//				.transactionManager(tm);
//		
//		return TrackingEventProcessor.builder().trackingEventProcessorConfiguration(tepConfig).name("MyProcessor").build();
	}

	@Autowired
	public void registerInterceptors(CommandBus commandbus) {
		commandbus.registerDispatchInterceptor(new BeanValidationInterceptor<>());
	}

	@Bean
	public SpringAggregateSnapshotterFactoryBean springAggregateSnapshotterFactoryBean() {
		return new SpringAggregateSnapshotterFactoryBean();
	}

	@Bean
	public EventTableFactory getEventTableFactory() {
		return PostgresEventTableFactory.INSTANCE;
	}

	@Bean
	public EventSchema getEventSchema() {
		return new EventSchema();
	}

	@Bean
	public SagaSqlSchema getSagaSqlSchema() {
		return new PostgresSagaSqlSchema();
	}

	@Autowired
	@Bean
	public KafkaMessageSource kafkaMessageSource(Fetcher fetcher) {
		return new KafkaMessageSource(fetcher);
	}

	@ConditionalOnMissingBean
	@Bean
	public KafkaMessageConverter<String, byte[]> kafkaMessageConverter(
			@Qualifier("eventSerializer") Serializer eventSerializer) {
		return DefaultKafkaMessageConverter.builder().serializer(eventSerializer).build();
	}

	@Bean
	public JpaTokenStore jpaTokenStore(Serializer serializer, EntityManagerProvider emp) {
		return JpaTokenStore.builder().entityManagerProvider(emp).serializer(serializer).build();
	}
}
