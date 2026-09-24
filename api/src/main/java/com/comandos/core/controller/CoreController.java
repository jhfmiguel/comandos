package com.comandos.core.controller;

import com.comandos.core.service.CoreCatalog;
import com.comandos.core.service.CoreService;
import com.comandos.core.service.CepService;
import com.comandos.security.service.AccessPolicy;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/core")
public class CoreController {
	
    private final CoreService service;
    private final AccessPolicy access;
    private final CepService cep;
    
    public CoreController(
    		CoreService service, 
    		AccessPolicy access,
            CepService cep
    ) { 
    	this.service = service; 
    	this.access = access;
        this.cep = cep;
    }

    
    @GetMapping("/postal-codes/{postalCode}")
    public Map<String, String> postalCode(@PathVariable String postalCode) {
        access.requireAny("core/people", "READ");
        return cep.lookup(postalCode);
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

    
    @PostMapping("/people/with-contacts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createPersonWithContacts(
            @RequestBody CoreService.PersonRegistration registration) {
        return service.savePersonWithContacts(registration);
    }

    @GetMapping("/{resource}")
    public CoreService.PageResult list(
    		@PathVariable String resource,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long organizationId,
            @RequestParam Map<String, String> params) {
        return service.list(resource, search, page, size, organizationId, params);
        
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
