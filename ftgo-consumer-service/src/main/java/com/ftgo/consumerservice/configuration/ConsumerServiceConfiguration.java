/*
 * Copyright 2020 Wuyi Chen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ftgo.consumerservice.configuration;

import io.eventuate.tram.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.sagas.participant.SagaParticipantConfiguration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.ftgo.common.configuration.CommonConfiguration;
import com.ftgo.consumerservice.service.ConsumerService;

/**
 * The configuration class to instantiate and wire the domain service class.
 * 
 * @author Wuyi Chen
 * @date 05/15/2020
 * @version 1.0
 * @since 1.0
 */
@Configuration
@EnableJpaRepositories
@EnableAutoConfiguration
@Import({ SagaParticipantConfiguration.class, TramEventsPublisherConfiguration.class, CommonConfiguration.class })
@EnableTransactionManagement
@ComponentScan
public class ConsumerServiceConfiguration {
	@Bean
	public ConsumerService consumerService() {
		return new ConsumerService();
	}
}
