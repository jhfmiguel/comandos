package com.weaponsregistration.rest.sales;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weaponsregistration.model.Sale;
import com.weaponsregistration.model.repository.SaleItemRepository;
import com.weaponsregistration.model.repository.SaleRepository;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin("*")
public class SalesController {
	
	private final SaleRepository saleRepository;
	private final SaleItemRepository saleItemRepository;

	SalesController(SaleRepository saleRepository, SaleItemRepository saleItemRepository) {
		this.saleRepository = saleRepository;
		this.saleItemRepository = saleItemRepository;
	}
	
	
	@PostMapping
	@Transactional 
	public void FinalizeSale( @RequestBody Sale sale ) {
		
		saleRepository.save( sale );
		
		sale.getWeapons().stream().forEach( salesItem -> salesItem.setSale(sale) );
		
		saleItemRepository.saveAll(sale.getWeapons());
		
	}

}
