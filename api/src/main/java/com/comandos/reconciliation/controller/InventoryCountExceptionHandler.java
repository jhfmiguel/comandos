package com.comandos.reconciliation.controller;
import java.util.Map;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes=InventoryCountController.class)public class InventoryCountExceptionHandler{@ExceptionHandler(ResponseStatusException.class)ResponseEntity<Map<String,Object>>handle(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("detail",e.getReason()==null?"Request failed.":e.getReason()));}}
