package com.weaponsregistration.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentMethod {
	
	CASH,
	PIX,
	CREDIT_CARD,
	DEBIT_CARD,
	BANK_TRANSFER;

	@JsonCreator
	public static PaymentMethod fromString(String value) {
		
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		
		return PaymentMethod.valueOf(value.toUpperCase().trim());
	}
	
}