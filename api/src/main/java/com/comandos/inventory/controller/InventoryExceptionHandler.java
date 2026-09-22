package com.comandos.inventory.controller;

import com.comandos.core.controller.CoreExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = InventoryController.class)
public class InventoryExceptionHandler extends CoreExceptionHandler { }
