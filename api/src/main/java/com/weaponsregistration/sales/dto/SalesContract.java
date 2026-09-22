package com.weaponsregistration.sales.dto;

import java.math.BigDecimal;
import java.util.List;
import com.weaponsregistration.model.PaymentMethod;

public final class SalesContract {
    
	private SalesContract() {}
    
	public record LineRequest(
			
			Long assetId, 
			Long balanceId, 
			BigDecimal quantity, 
			BigDecimal expectedUnitPrice
		
	) {}
    
	public record FinalizeRequest(
			
			String requestId, 
			Long organizationId, 
			Long buyerId, 
			PaymentMethod paymentMethod, 
			List<LineRequest> items,
            Long unitId,
            String processNumber,
            String legalBasis,
            String documentReference
		
	) {}
    
	public record StockOption(
		
			String kind, 
			long stockId, 
			String code, 
			String modelName, 
			String sku,
			String locationName, 
			String unitOfMeasure, 
			String available, 
			String unitPrice
		
	) {}
	
    public record Page<T>(
    		
    		List<T> content, 
    		long totalElements, 
    		int page, 
    		int size
    	
    ) {}
    
    public record LineView(
    		
    		long id, 
    		long modelId, 
    		Long assetId, 
    		Long lotId, 
    		long locationId, 
    		long movementId,
    		String modelName, 
    		String sku, 
    		String stockCode, 
    		String locationName, 
    		String unitOfMeasure,
    		String quantity, 
    		String unitPrice, 
    		String subtotal
        
      ) {}

    public record ReturnLineRequest(Long saleItemId, BigDecimal quantity) {}
    public record ReturnRequest(String requestId, Long reasonId, String notes, String refundReference,
            boolean cancellation, List<ReturnLineRequest> items) {}
    public record ReturnLineView(long id, long saleItemId, String stockCode, String modelName,
            String quantity, String refundAmount, long movementId) {}
    public record ReturnView(long id, long reasonId, String reasonCode, String reasonName,
            boolean cancellation, String notes, String returnedAt, String refundAmount,
            String refundReference, String operatorLogin, List<ReturnLineView> items) {}
    
    public record SaleView(
    		
    		long id, 
    		long organizationId, 
    		String organizationName, 
    		long buyerId, 
    		String buyerName,
    		String paymentMethod, 
    		String status, 
    		String finalizedAt, 
    		String total, 
    		List<LineView> items,
            Long finalizedById,
            String finalizedByLogin,
            Long unitId,
            String unitName,
            List<ReturnView> returns

     		
     	) {}
    
}
