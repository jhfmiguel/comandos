package com.weaponsregistration.inventory.controller;

import com.weaponsregistration.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = InventoryController.class)
public class InventoryExceptionHandler extends CoreExceptionHandler { }
