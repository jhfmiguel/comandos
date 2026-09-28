package com.comandos.lifecycle.controller;
import com.comandos.lifecycle.dto.LifecycleContract.*;import com.comandos.lifecycle.service.ExceptionalOccurrenceService;import com.comandos.lifecycle.service.InspectionApprovalService;import com.comandos.lifecycle.service.InspectionChecklistService;import com.comandos.lifecycle.service.LifecycleService;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController@RequestMapping("/api/erp/lifecycle")public class LifecycleController{
 private final LifecycleService service;private final InspectionChecklistService checklist;private final InspectionApprovalService approval;private final ExceptionalOccurrenceService occurrences;
 public LifecycleController(LifecycleService service,InspectionChecklistService checklist,InspectionApprovalService approval,ExceptionalOccurrenceService occurrences){this.service=service;this.checklist=checklist;this.approval=approval;this.occurrences=occurrences;}
 @PostMapping("/inspections")@ResponseStatus(HttpStatus.CREATED)public InspectionView inspect(@RequestBody InspectionRequest r){return service.inspect(r);}
 @PostMapping("/inspections/{id}/approve")public InspectionView approve(@PathVariable long id){return approval.approve(id);}
 @GetMapping("/inspections")public Page<InspectionView> inspections(@RequestParam long organizationId,@RequestParam(required=false)Long unitId,@RequestParam(defaultValue="0")int page){return service.inspections(organizationId,unitId,page);}
 @PutMapping("/inspections/{id}/checklist")public List<InspectionItemView> checklist(@PathVariable long id,@RequestBody InspectionChecklistRequest r){return checklist.save(id,r);}
 @GetMapping("/inspections/{id}/checklist")public List<InspectionItemView> checklist(@PathVariable long id){return checklist.get(id);}
 @PostMapping("/inspections/{id}/checklist/{itemId}/photos")@ResponseStatus(HttpStatus.CREATED)public AttachmentView checklistPhoto(@PathVariable long id,@PathVariable long itemId,@RequestBody AttachmentRequest r){return checklist.addPhoto(id,itemId,r);}
 @PostMapping("/occurrences")@ResponseStatus(HttpStatus.CREATED)public OccurrenceView occur(@RequestBody OccurrenceRequest r){return occurrences.occur(r);}
 @PostMapping("/occurrences/{id}/investigate")public OccurrenceView investigate(@PathVariable long id,@RequestBody ResolveRequest r){return occurrences.investigate(id,r);}
 @PostMapping("/occurrences/{id}/resolve")public OccurrenceView resolve(@PathVariable long id,@RequestBody(required=false)ResolveRequest r){return occurrences.resolve(id,r);}
 @GetMapping("/occurrences")public Page<OccurrenceView> occurrenceList(@RequestParam long organizationId,@RequestParam(required=false)Long unitId,@RequestParam(defaultValue="0")int page){return occurrences.list(organizationId,unitId,page);}
 @PostMapping("/attachments")@ResponseStatus(HttpStatus.CREATED)public AttachmentView attach(@RequestBody AttachmentRequest r){return service.attach(r);}
 @GetMapping("/attachments")public List<AttachmentView> attachments(@RequestParam String resource,@RequestParam long recordId){return service.attachments(resource,recordId);}
 @GetMapping("/attachments/{id}/content")public ResponseEntity<byte[]> content(@PathVariable long id){var a=service.attachment(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.contentType)).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+a.fileName.replace("\"","")+"\"").body(a.content);}
}
