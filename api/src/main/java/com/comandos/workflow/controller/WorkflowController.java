package com.comandos.workflow.controller;
import com.comandos.workflow.dto.WorkflowContract.*;import com.comandos.workflow.service.WorkflowService;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController@RequestMapping("/api/erp/workflows")public class WorkflowController{private final WorkflowService s;public WorkflowController(WorkflowService s){this.s=s;}
 @PostMapping@ResponseStatus(HttpStatus.CREATED)public View request(@RequestBody Request r){return s.request(r);}@GetMapping("/{id}")public View get(@PathVariable long id){return s.get(id);}
 @GetMapping public Page<View>list(@RequestParam long organizationId,@RequestParam(required=false)Long unitId,@RequestParam(required=false)String status,@RequestParam(defaultValue="0")int page){return s.list(organizationId,unitId,status,page);}
 @PostMapping("/{id}/analyze")public View analyze(@PathVariable long id,@RequestBody(required=false)Transition r){return s.analyze(id,r);}@PostMapping("/{id}/authorize")public View authorize(@PathVariable long id,@RequestBody(required=false)Transition r){return s.authorize(id,r);}
 @PostMapping("/{id}/execute")public View execute(@PathVariable long id,@RequestBody(required=false)Transition r){return s.execute(id,r);}@PostMapping("/{id}/conclude")public View conclude(@PathVariable long id,@RequestBody(required=false)Transition r){return s.conclude(id,r);}
 @PostMapping("/{id}/cancel")public View cancel(@PathVariable long id,@RequestBody(required=false)Transition r){return s.cancel(id,r);}
}