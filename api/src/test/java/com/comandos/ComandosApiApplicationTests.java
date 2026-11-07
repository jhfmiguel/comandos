package com.comandos;

import static org.assertj.core.api.Assertions.assertThat;

import com.fariamiguel.enterprise.persistence.jpa.JdbcSalesOperationsDirectory;
import com.fariamiguel.enterprise.sales.SalesOperationsDirectory;
import com.fariamiguel.enterprise.sales.SalesOperationsService;
import com.fariamiguel.enterprise.sales.SalesOrderFulfillmentProvider;
import com.comandos.sales.service.ComandosSalesOrderFulfillmentProvider;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ComandosApiApplicationTests {
	@Autowired SalesOperationsService salesOperations;
	@Autowired SalesOrderFulfillmentProvider salesOrderFulfillment;

	@Test
	void contextLoads() {
	}

	@Test
	void genericSalesServiceUsesTheDurablePlatformStoreAdapter() {
		assertThat(salesOperations.directory()).isInstanceOf(JdbcSalesOperationsDirectory.class);
	}

	@Test
	void genericSalesRecordsCanBeWrittenAndReadThroughTheDurableAdapter() {
		var directory = salesOperations.directory();
		var target = new SalesOperationsDirectory.SalesTarget(
			"comandos-sales-persistence-check", "comandos", "seller-1",
			SalesOperationsDirectory.SalesParticipantType.SELLER, "2026-11",
			new BigDecimal("1000.00"), "BRL", true, Map.of("source", "integration-test"));

		assertThat(directory.save(target)).isEqualTo(target);
		assertThat(directory.find("comandos", target.id(), SalesOperationsDirectory.SalesTarget.class))
			.contains(target);
	}

	@Test
	void comandosProvidesTheScopedInventoryFulfillmentAdapter() {
		assertThat(salesOrderFulfillment).isInstanceOf(ComandosSalesOrderFulfillmentProvider.class);
	}
}
