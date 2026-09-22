package com.weaponsregistration.lifecycle.controller;
import com.weaponsregistration.lifecycle.dto.LifecycleContract.*;import com.weaponsregistration.lifecycle.service.LifecycleService;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController@RequestMapping("/api/erp/lifecycle")public class LifecycleController{
 private final LifecycleService service;public LifecycleController(LifecycleService service){this.service=service;}
 @PostMapping("/inspections")@ResponseStatus(HttpStatus.CREATED)public InspectionView inspect(@RequestBody InspectionRequest r){return service.inspect(r);}
 @PostMapping("/inspections/{id}/approve")public InspectionView approve(@PathVariable long id){return service.approve(id);}
 @GetMapping("/inspections")public Page<InspectionView> inspections(@RequestParam long organizationId,@RequestParam(required=false)Long unitId,@RequestParam(defaultValue="0")int page){return service.inspections(organizationId,unitId,page);}
 @PostMapping("/occurrences")@ResponseStatus(HttpStatus.CREATED)public OccurrenceView occur(@RequestBody OccurrenceRequest r){return service.occur(r);}
 @PostMapping("/occurrences/{id}/resolve")public OccurrenceView resolve(@PathVariable long id,@RequestBody(required=false)ResolveRequest r){return service.resolve(id,r);}
 @GetMapping("/occurrences")public Page<OccurrenceView> occurrences(@RequestParam long organizationId,@RequestParam(required=false)Long unitId,@RequestParam(defaultValue="0")int page){return service.occurrences(organizationId,unitId,page);}
 @PostMapping("/attachments")@ResponseStatus(HttpStatus.CREATED)public AttachmentView attach(@RequestBody AttachmentRequest r){return service.attach(r);}
 @GetMapping("/attachments")public List<AttachmentView> attachments(@RequestParam String resource,@RequestParam long recordId){return service.attachments(resource,recordId);}
 @GetMapping("/attachments/{id}/content")public ResponseEntity<byte[]> content(@PathVariable long id){var a=service.attachment(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.contentType)).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+a.fileName.replace("\"","")+"\"").body(a.content);}
}