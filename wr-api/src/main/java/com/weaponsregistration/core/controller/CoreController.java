package com.weaponsregistration.core.controller;

import com.weaponsregistration.core.service.CoreCatalog;
import com.weaponsregistration.core.service.CoreService;
import com.weaponsregistration.security.service.AccessPolicy;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/core")
public class CoreController {
	
    private final CoreService service;
    private final AccessPolicy access;
    
    public CoreController(
    		CoreService service, 
    		AccessPolicy access
    ) { 
    	this.service = service; 
    	this.access = access; 
    }

    
    @GetMapping("/catalog")
    public List<Map<String, Object>> catalog() {
        
    	return CoreCatalog.RESOURCES.stream()
    			.filter(r -> access.canAny("core/" + r.key(), "READ"))
    			.map(r -> Map.<String, Object>of(
    					"key", r.key(), 
    					"label", r.label(), 
    					"group", r.group(), 
    					"fields", r.fields(), 
    					"actions", access.actions("core/" + r.key()))).toList();
    }

    
    @GetMapping("/{resource}")
    public CoreService.PageResult list(
    		@PathVariable String resource,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long organizationId) {
    	
        return service.list(resource, search, page, size, organizationId);
        
    }

    
    @GetMapping("/{resource}/{id}")
    public Map<String, Object> get(
    		@PathVariable String resource, 
    		@PathVariable long id) {
    	
        return service.get(resource, id);
        
    }

    @PostMapping("/{resource}") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(
    		@PathVariable String resource, 
    		@RequestBody Map<String, Object> data) {
        
    	return service.save(resource, null, data);
    	
    }

    
    @PutMapping("/{resource}/{id}")
    public Map<String, Object> update(
    		@PathVariable String resource, 
    		@PathVariable long id,
            @RequestBody Map<String, Object> data) {
    	
        return service.save(resource, id, data);
        
    }

    @DeleteMapping("/{resource}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
    		@PathVariable String resource, 
    		@PathVariable long id, 
    		@RequestParam Long version) {
    	
        service.delete(resource, id, version);
   
    }
    
}
