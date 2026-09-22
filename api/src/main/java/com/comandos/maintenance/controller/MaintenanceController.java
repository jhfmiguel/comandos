package com.comandos.maintenance.controller;
import com.comandos.maintenance.dto.MaintenanceContract.*;import com.comandos.maintenance.service.MaintenanceService;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/erp/maintenance") public class MaintenanceController{
 private final MaintenanceService service;public MaintenanceController(MaintenanceService service){this.service=service;}
 @PostMapping("/plans")@ResponseStatus(HttpStatus.CREATED)public PlanView plan(@RequestBody PlanRequest request){return service.plan(request);}
 @GetMapping("/plans")public Page<PlanView>plans(@RequestParam long organizationId,@RequestParam(required=false)Long unitId){return service.plans(organizationId,unitId);}
 @GetMapping("/assets")public Page<AssetOption>assets(@RequestParam long organizationId,@RequestParam(required=false)Long unitId,@RequestParam(defaultValue="")String search){return service.assets(organizationId,unitId,search);}
 @PostMapping("/orders")public OrderView open(@RequestBody OpenRequest request){return service.open(request);}
 @PostMapping("/orders/{id}/complete")public OrderView complete(@PathVariable long id,@RequestBody CompleteRequest request){return service.complete(id,request);}
 @GetMapping("/orders")public Page<OrderView>orders(@RequestParam long organizationId,@RequestParam(required=false)Long unitId,@RequestParam(defaultValue="0")int page){return service.orders(organizationId,unitId,page);}
}
