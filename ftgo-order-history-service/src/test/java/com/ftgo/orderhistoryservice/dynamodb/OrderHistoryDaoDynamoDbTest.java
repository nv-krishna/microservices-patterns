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
package com.ftgo.orderhistoryservice.dynamodb;

import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.tram.inmemory.TramInMemoryConfiguration;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ftgo.common.model.Money;
import com.ftgo.orderhistoryservice.dao.OrderHistoryDao;
import com.ftgo.orderhistoryservice.dao.dynamodb.OrderHistoryDynamoDBConfiguration;
import com.ftgo.orderhistoryservice.dao.dynamodb.SourceEvent;
import com.ftgo.orderhistoryservice.domain.OrderHistoryFilter;
import com.ftgo.orderhistoryservice.model.Order;
import com.ftgo.orderhistoryservice.model.OrderHistory;
import com.ftgo.orderservice.api.model.OrderLineItem;
import com.ftgo.orderservice.api.model.OrderState;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { OrderHistoryDaoDynamoDbTest.OrderHistoryDaoDynamoDbTestConfiguration.class })
public class OrderHistoryDaoDynamoDbTest {
	@Configuration
	@EnableAutoConfiguration
	@ComponentScan
	@Import({ OrderHistoryDynamoDBConfiguration.class, TramInMemoryConfiguration.class })
	static public class OrderHistoryDaoDynamoDbTestConfiguration {

	}

	private String                consumerId;
	private Order                 order1;
	private String                orderId;
	@Autowired
	private OrderHistoryDao       dao;
	private String                restaurantName;
	private String                chickenVindaloo;
	private Optional<SourceEvent> eventSource;
	private long                  restaurantId;

	@Before
	public void setup() {
		consumerId      = "consumerId" + System.currentTimeMillis();
		orderId         = "orderId" + System.currentTimeMillis();
		restaurantName  = "Ajanta" + System.currentTimeMillis();
		chickenVindaloo = "Chicken Vindaloo" + System.currentTimeMillis();
		restaurantId    = 101L;

		order1 = new Order(orderId, consumerId, OrderState.APPROVAL_PENDING,
				singletonList(new OrderLineItem("-1", chickenVindaloo, Money.ZERO, 0)), null, restaurantId,
				restaurantName);
		order1.setCreationDate(DateTime.now().minusDays(5));
		eventSource = Optional.of(new SourceEvent("Order", orderId, "11212-34343"));

		dao.addOrder(order1, eventSource);
	}

	@Test
	public void shouldFindOrder() {
		Optional<Order> order = dao.findOrder(orderId);
		assertOrderEquals(order1, order.get());
	}

	@Test
	public void shouldIgnoreDuplicateAdd() {
		dao.cancelOrder(orderId, Optional.empty());
		assertFalse(dao.addOrder(order1, eventSource));
		Optional<Order> order = dao.findOrder(orderId);
		assertEquals(OrderState.CANCELLED, order.get().getStatus());
	}

	@Test
	public void shouldFindOrders() {
		OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter());
		assertNotNull(result);
		List<Order> orders = result.getOrders();
		Order retrievedOrder = assertContainsOrderId(orderId, orders);
		assertOrderEquals(order1, retrievedOrder);
	}

	private void assertOrderEquals(Order expected, Order other) {
		System.out.println("Expected=" + JSonMapper.toJson(expected.getLineItems()));
		System.out.println("actual  =" + JSonMapper.toJson(other.getLineItems()));
		assertEquals(expected.getLineItems(), other.getLineItems());
		assertEquals(expected.getStatus(), other.getStatus());
		assertEquals(expected.getCreationDate(), other.getCreationDate());
		assertEquals(expected.getRestaurantId(), other.getRestaurantId());
		assertEquals(expected.getRestaurantName(), other.getRestaurantName());
	}

	@Test
	public void shouldFindOrdersWithStatus() throws InterruptedException {
		OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withStatus(OrderState.APPROVAL_PENDING));
		assertNotNull(result);
		List<Order> orders = result.getOrders();
		assertContainsOrderId(orderId, orders);
	}

	@Test
	public void shouldCancel() throws InterruptedException {
		dao.cancelOrder(orderId, Optional.of(new SourceEvent("a", "b", "c")));
		Order order = dao.findOrder(orderId).get();
		assertEquals(OrderState.CANCELLED, order.getStatus());
	}

	@Test
	public void shouldHandleCancel() throws InterruptedException {
		assertTrue(dao.cancelOrder(orderId, Optional.of(new SourceEvent("a", "b", "c"))));
		assertFalse(dao.cancelOrder(orderId, Optional.of(new SourceEvent("a", "b", "c"))));
	}

	@Test
	public void shouldFindOrdersWithCancelledStatus() {
		OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withStatus(OrderState.CANCELLED));
		assertNotNull(result);
		List<Order> orders = result.getOrders();
		assertNotContainsOrderId(orderId, orders);
	}

	@Test
	public void shouldFindOrderByMenuItem() {
		OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withKeywords(singleton(chickenVindaloo)));
		assertNotNull(result);
		List<Order> orders = result.getOrders();
		assertContainsOrderId(orderId, orders);
	}

	@Test
	public void shouldReturnOrdersSorted() {
		String orderId2 = "orderId" + System.currentTimeMillis();
		Order order2 = new Order(orderId2, consumerId, OrderState.APPROVAL_PENDING, singletonList(new OrderLineItem("-1", "Lamb 65", Money.ZERO, -1)), null, restaurantId, restaurantName);
		order2.setCreationDate(DateTime.now().minusDays(1));
		dao.addOrder(order2, eventSource);
		OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter());
		List<Order> orders = result.getOrders();

		int idx1 = indexOf(orders, orderId);
		int idx2 = indexOf(orders, orderId2);
		assertTrue(idx2 < idx1);
	}

	private int indexOf(List<Order> orders, String orderId2) {
		Order order = orders.stream().filter(o -> o.getOrderId().equals(orderId2)).findFirst().get();
		return orders.indexOf(order);
	}

	private Order assertContainsOrderId(String orderId, List<Order> orders) {
		Optional<Order> order = orders.stream().filter(o -> o.getOrderId().equals(orderId)).findFirst();
		assertTrue("Order not found", order.isPresent());
		return order.get();
	}

	private void assertNotContainsOrderId(String orderId, List<Order> orders) {
		Optional<Order> order = orders.stream().filter(o -> o.getOrderId().equals(orderId)).findFirst();
		assertFalse(order.isPresent());
	}

	@Test
	public void shouldPaginateResults() {
		String orderId2 = "orderId" + System.currentTimeMillis();
		Order order2 = new Order(orderId2, consumerId, OrderState.APPROVAL_PENDING, singletonList(new OrderLineItem("-1", "Lamb 65", Money.ZERO, -1)), null, restaurantId, restaurantName);
		order2.setCreationDate(DateTime.now().minusDays(1));
		dao.addOrder(order2, eventSource);

		OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withPageSize(1));

		assertEquals(1, result.getOrders().size());
		assertTrue(result.getStartKey().isPresent());

		OrderHistory result2 = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withPageSize(1).withStartKeyToken(result.getStartKey()));

		assertEquals(1, result.getOrders().size());
	}
}