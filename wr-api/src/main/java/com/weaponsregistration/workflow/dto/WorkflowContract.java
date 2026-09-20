package com.weaponsregistration.workflow.dto;
import java.util.List;public final class WorkflowContract{private WorkflowContract(){}
 public record Request(Long organizationId,Long unitId,String operationType,String resource,Long recordId,String justification){}
 public record Transition(String justification){}
 public record EventView(Long id,String fromStatus,String toStatus,String justification,String occurredAt,String actorLogin){}
 public record View(Long id,Long organizationId,Long unitId,String operationType,String resource,Long recordId,String status,String justification,String requestedAt,String requestedByLogin,String authorityLogin,String authorizedAt,String executedAt,String concludedAt,String cancelledAt,List<EventView>events){}
 public record Page<T>(List<T>content,long totalElements,int page,int size){}
}