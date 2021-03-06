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
package com.ftgo.kitchenservice.event;

import com.ftgo.kitchenservice.api.event.model.TicketDomainEvent;
import com.ftgo.kitchenservice.model.Ticket;

import io.eventuate.tram.events.aggregates.AbstractAggregateDomainEventPublisher;
import io.eventuate.tram.events.publisher.DomainEventPublisher;

/**
 * The class for publishing Ticket aggregatesâ€™ domain events.
 * 
 * <p>This class only publishes events that are a subclass of {@code TicketDomainEvent}.
 *
 * @author  Wuyi Chen
 * @date    04/14/2020
 * @version 1.0
 * @since   1.0
 */
public class TicketDomainEventPublisher extends AbstractAggregateDomainEventPublisher<Ticket, TicketDomainEvent> {
	public TicketDomainEventPublisher(DomainEventPublisher eventPublisher) {
		super(eventPublisher, Ticket.class, Ticket::getId);
	}
}
