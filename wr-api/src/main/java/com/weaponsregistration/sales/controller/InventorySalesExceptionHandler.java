package com.weaponsregistration.sales.controller;

import com.weaponsregistration.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = InventorySalesController.class)
public class InventorySalesExceptionHandler extends CoreExceptionHandler {}
