package com.comandos.contracts;

import com.comandos.security.service.ComandosAccessPolicy;
import com.fariamiguel.enterprise.common.BusinessId;
import com.fariamiguel.enterprise.common.Money;
import com.fariamiguel.enterprise.contracts.Contract;
import com.fariamiguel.enterprise.contracts.ContractLegalDirectory;
import com.fariamiguel.enterprise.contracts.ContractLegalService;
import com.fariamiguel.enterprise.contracts.ContractStatus;
import com.fariamiguel.security.api.CurrentActorProvider;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** COMANDOS-scoped adapter for generic contracts and legal operations. */
@RestController
@RequestMapping("/api/erp/contracts")
@Transactional
public class ComandosContractLegalController {
    private static final String TENANT = "comandos";
    private final ContractLegalService service;
    private final ComandosAccessPolicy access;
    private final CurrentActorProvider actors;

    public ComandosContractLegalController(ContractLegalService service, ComandosAccessPolicy access,
            CurrentActorProvider actors) {
        this.service = Objects.requireNonNull(service);
        this.access = Objects.requireNonNull(access);
        this.actors = Objects.requireNonNull(actors);
    }

    @GetMapping
    public List<Contract> contracts(@RequestParam long organizationId,@RequestParam(required=false) Long unitId) {
        require("READ",organizationId,unitId);
        return service.listContracts(TENANT).stream().filter(value->within(value.references(),organizationId,unitId)).toList();
    }

    @PostMapping
    public Contract create(@RequestBody CreateContract body) {
        require("CREATE",body.organizationId(),body.unitId());
        Map<String,String> references=new HashMap<>(body.references()==null?Map.of():body.references());
        references.put("organizationId",Long.toString(body.organizationId()));
        if(body.unitId()==null)references.remove("unitId");else references.put("unitId",Long.toString(body.unitId()));
        Money value=body.value()==null?null:new Money(body.value(),Currency.getInstance(body.currency()==null?"BRL":body.currency()));
        Contract contract=new Contract(BusinessId.of(body.id()),TenantId.of(TENANT),CompanyId.of(Long.toString(body.organizationId())),
                body.counterpartyId()==null?null:BusinessId.of(body.counterpartyId()),body.number(),body.subject(),ContractStatus.DRAFT,
                body.startsAt(),body.endsAt(),value,references);
        return service.createContract(contract,body.partyRole(),actor());
    }

    @GetMapping("/{id}")
    public Contract contract(@PathVariable String id,@RequestParam long organizationId,@RequestParam(required=false) Long unitId) {
        require("READ",organizationId,unitId);
        Contract value=service.findContract(TENANT,id).orElseThrow(()->new IllegalArgumentException("contract does not exist"));
        requireOwned(value.references(),organizationId,unitId);
        return value;
    }

    @PostMapping("/{id}/activate")
    public Contract activate(@PathVariable String id,@RequestParam long organizationId,@RequestParam(required=false) Long unitId) {
        require("APPROVE",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.activateContract(TENANT,id,actor());
    }

    @PostMapping("/{id}/amendments")
    public ContractLegalDirectory.Amendment amend(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestBody AmendmentRequest body) {
        require("UPDATE",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.requestAmendment(TENANT,id,body.summary(),body.effectiveOn(),body.newEndDate(),body.newValue(),
                body.currency(),actor(),body.metadata());
    }

    @GetMapping("/{id}/amendments")
    public List<ContractLegalDirectory.Amendment> amendments(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId) {
        require("READ",organizationId,unitId);
        requireOwned(contract(id).references(),organizationId,unitId);
        return service.directory().list(TENANT,ContractLegalDirectory.Amendment.class).stream()
                .filter(value->value.contractId().equals(id))
                .sorted(java.util.Comparator.comparingInt(ContractLegalDirectory.Amendment::sequence)).toList();
    }

    @GetMapping("/{id}/history")
    public List<ContractLegalDirectory.ContractStatusTransition> contractHistory(@PathVariable String id,
            @RequestParam long organizationId,@RequestParam(required=false) Long unitId) {
        require("READ",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.directory().list(TENANT,ContractLegalDirectory.ContractStatusTransition.class).stream()
                .filter(value->value.contractId().equals(id))
                .sorted(java.util.Comparator.comparing(ContractLegalDirectory.ContractStatusTransition::recordedAt)).toList();
    }

    @GetMapping("/{id}/adjustments")
    public List<ContractLegalDirectory.ContractAdjustment> adjustments(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId) {
        require("READ",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.directory().list(TENANT,ContractLegalDirectory.ContractAdjustment.class).stream()
                .filter(value->value.contractId().equals(id))
                .sorted(java.util.Comparator.comparing(ContractLegalDirectory.ContractAdjustment::effectiveOn)).toList();
    }

    @PostMapping("/{id}/adjustments")
    public ContractLegalDirectory.ContractAdjustment requestAdjustment(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestBody AdjustmentRequest body) {
        require("FINANCE",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.requestAdjustment(TENANT,id,body.indexCode(),body.ratePercent(),body.effectiveOn(),actor(),body.metadata());
    }

    @PostMapping("/adjustments/{adjustmentId}/decision")
    public ContractLegalDirectory.ContractAdjustment decideAdjustment(@PathVariable String adjustmentId,
            @RequestParam long organizationId,@RequestParam(required=false) Long unitId,@RequestBody Decision body) {
        require("FINANCE",organizationId,unitId);
        var adjustment=service.directory().find(TENANT,adjustmentId,ContractLegalDirectory.ContractAdjustment.class)
                .orElseThrow(()->new IllegalArgumentException("contract adjustment does not exist"));
        requireOwned(contract(adjustment.contractId()).references(),organizationId,unitId);
        return service.decideAdjustment(TENANT,adjustmentId,body.approve(),actor(),body.reason());
    }

    @PostMapping("/{id}/adjustments/apply-due")
    public List<ContractLegalDirectory.ContractAdjustment> applyDueAdjustments(@PathVariable String id,
            @RequestParam long organizationId,@RequestParam(required=false) Long unitId) {
        require("FINANCE",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.applyDueAdjustments(TENANT,id);
    }

    @PostMapping("/{id}/renewals")
    public ContractLegalDirectory.Renewal requestRenewal(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestBody RenewalRequest body) {
        require("UPDATE",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.requestRenewal(TENANT,id,body.startOn(),body.endOn(),actor(),body.metadata());
    }

    @GetMapping("/{id}/renewals")
    public List<ContractLegalDirectory.Renewal> renewals(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId) {
        require("READ",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.directory().list(TENANT,ContractLegalDirectory.Renewal.class).stream()
                .filter(value->value.contractId().equals(id))
                .sorted(java.util.Comparator.comparing(ContractLegalDirectory.Renewal::requestedStartOn)).toList();
    }

    @PostMapping("/renewals/{renewalId}/decision")
    public ContractLegalDirectory.Renewal decideRenewal(@PathVariable String renewalId,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestBody Decision body) {
        require("APPROVE",organizationId,unitId);
        var renewal=service.directory().find(TENANT,renewalId,ContractLegalDirectory.Renewal.class)
                .orElseThrow(()->new IllegalArgumentException("renewal does not exist"));
        requireOwned(contract(renewal.contractId()).references(),organizationId,unitId);
        return service.decideRenewal(TENANT,renewalId,body.approve(),actor(),body.reason());
    }

    @PostMapping("/renewals/{renewalId}/complete")
    public ContractLegalDirectory.Renewal completeRenewal(@PathVariable String renewalId,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId) {
        require("APPROVE",organizationId,unitId);
        var renewal=service.directory().find(TENANT,renewalId,ContractLegalDirectory.Renewal.class)
                .orElseThrow(()->new IllegalArgumentException("renewal does not exist"));
        requireOwned(contract(renewal.contractId()).references(),organizationId,unitId);
        return service.completeRenewal(TENANT,renewalId,actor());
    }

    @PostMapping("/{id}/obligations")
    public ContractLegalDirectory.Obligation saveObligation(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestBody ObligationRequest body) {
        require("UPDATE",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.saveObligation(new ContractLegalDirectory.Obligation(body.id(),TENANT,id,body.code(),body.description(),
                body.dueOn(),body.responsibleParty(),ContractLegalDirectory.ObligationStatus.OPEN,Instant.now(),null,
                body.notes(),body.metadata()));
    }

    @GetMapping("/{id}/obligations")
    public List<ContractLegalDirectory.Obligation> obligations(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId) {
        require("READ",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.obligations(TENANT,id);
    }

    @PostMapping("/obligations/{obligationId}/status")
    public ContractLegalDirectory.Obligation updateObligation(@PathVariable String obligationId,
            @RequestParam long organizationId,@RequestParam(required=false) Long unitId,@RequestBody ObligationUpdate body) {
        require("UPDATE",organizationId,unitId);
        var obligation=service.directory().find(TENANT,obligationId,ContractLegalDirectory.Obligation.class)
                .orElseThrow(()->new IllegalArgumentException("obligation does not exist"));
        requireOwned(contract(obligation.contractId()).references(),organizationId,unitId);
        return service.updateObligation(TENANT,obligationId,body.status(),body.evidenceReference(),body.notes());
    }

    @PostMapping("/amendments/{amendmentId}/decision")
    public ContractLegalDirectory.Amendment decideAmendment(@PathVariable String amendmentId,
            @RequestParam long organizationId,@RequestParam(required=false) Long unitId,@RequestBody Decision body) {
        require("APPROVE",organizationId,unitId);
        var amendment=service.directory().find(TENANT,amendmentId,ContractLegalDirectory.Amendment.class)
                .orElseThrow(()->new IllegalArgumentException("amendment does not exist"));
        requireOwned(contract(amendment.contractId()).references(),organizationId,unitId);
        return service.decideAmendment(TENANT,amendmentId,body.approve(),actor(),body.reason());
    }

    @GetMapping("/{id}/risk-assessments")
    public List<ContractLegalDirectory.ContractRiskAssessment> risks(@PathVariable String id,
            @RequestParam long organizationId,@RequestParam(required=false) Long unitId) {
        require("READ",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.riskAssessments(TENANT,id);
    }

    @PostMapping("/{id}/risk-assessments")
    public ContractLegalDirectory.ContractRiskAssessment assessRisk(@PathVariable String id,
            @RequestParam long organizationId,@RequestParam(required=false) Long unitId,@RequestBody RiskRequest body) {
        require("UPDATE",organizationId,unitId);requireOwned(contract(id).references(),organizationId,unitId);
        return service.assessRisk(new ContractLegalDirectory.ContractRiskAssessment(body.id(),TENANT,id,body.reference(),
                body.category(),body.description(),body.likelihood(),body.impact(),body.exposureAmount(),body.currency(),
                body.mitigation(),body.owner(),body.status()==null?ContractLegalDirectory.ContractRiskStatus.OPEN:body.status(),
                actor(),Instant.now(),body.metadata()));
    }

    @GetMapping("/legal/matters")
    public List<ContractLegalDirectory.LegalMatter> matters(@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestParam(required=false) ContractLegalDirectory.LegalMatterStatus status) {
        require("READ","legal/matters",organizationId,unitId);
        return service.matters(TENANT,status).stream().filter(value->within(value.metadata(),organizationId,unitId)).toList();
    }

    @PostMapping("/legal/matters/{id}/close")
    public ContractLegalDirectory.LegalMatter closeMatter(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestBody CloseMatterRequest body) {
        require("APPROVE","legal/matters",organizationId,unitId);requireOwned(matter(id).metadata(),organizationId,unitId);
        return service.closeMatter(TENANT,id,body.closedOn(),body.outcome());
    }

    @GetMapping("/legal/law-firms")
    public List<ContractLegalDirectory.LawFirm> lawFirms(@RequestParam long organizationId,@RequestParam(required=false) Long unitId) {
        require("READ","legal/matters",organizationId,unitId);
        return service.directory().list(TENANT,ContractLegalDirectory.LawFirm.class).stream()
                .filter(value->within(value.metadata(),organizationId,unitId))
                .sorted(java.util.Comparator.comparing(ContractLegalDirectory.LawFirm::name)).toList();
    }

    @PostMapping("/legal/law-firms")
    public ContractLegalDirectory.LawFirm saveLawFirm(@RequestBody LawFirmRequest body) {
        require("CREATE","legal/matters",body.organizationId(),body.unitId());
        Map<String,String> metadata=scopeMetadata(body.metadata(),body.organizationId(),body.unitId());
        return service.saveLawFirm(new ContractLegalDirectory.LawFirm(body.id(),TENANT,body.code(),body.name(),
                body.registrationNumber(),body.email(),body.phone(),body.active(),metadata));
    }

    @GetMapping("/legal/powers-of-attorney")
    public List<ContractLegalDirectory.PowerOfAttorney> powersOfAttorney(@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId) {
        require("READ","legal/authorities",organizationId,unitId);
        return service.directory().list(TENANT,ContractLegalDirectory.PowerOfAttorney.class).stream()
                .filter(value->within(value.metadata(),organizationId,unitId))
                .sorted(java.util.Comparator.comparing(ContractLegalDirectory.PowerOfAttorney::issuedOn).reversed()).toList();
    }

    @PostMapping("/legal/powers-of-attorney")
    public ContractLegalDirectory.PowerOfAttorney savePowerOfAttorney(@RequestBody PowerOfAttorneyRequest body) {
        require("CREATE","legal/authorities",body.organizationId(),body.unitId());
        return service.savePowerOfAttorney(new ContractLegalDirectory.PowerOfAttorney(body.id(),TENANT,body.reference(),
                body.principalId(),body.attorneyName(),body.attorneyRegistration(),body.scope(),body.issuedOn(),body.expiresOn(),
                ContractLegalDirectory.AuthorityStatus.ACTIVE,body.documentReference(),null,null,null,
                scopeMetadata(body.metadata(),body.organizationId(),body.unitId())));
    }

    @PostMapping("/legal/powers-of-attorney/{id}/revoke")
    public ContractLegalDirectory.PowerOfAttorney revokePowerOfAttorney(@PathVariable String id,
            @RequestParam long organizationId,@RequestParam(required=false) Long unitId,@RequestBody RevokeAuthorityRequest body) {
        require("APPROVE","legal/authorities",organizationId,unitId);
        var authority=service.directory().find(TENANT,id,ContractLegalDirectory.PowerOfAttorney.class)
                .orElseThrow(()->new IllegalArgumentException("power of attorney does not exist"));
        requireOwned(authority.metadata(),organizationId,unitId);
        return service.revokePowerOfAttorney(TENANT,id,actor(),body.reason());
    }

    @GetMapping("/legal/opinions")
    public List<ContractLegalDirectory.LegalOpinion> opinions(@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestParam(required=false) String matterId,
            @RequestParam(required=false) String contractId) {
        require("READ","legal/opinions",organizationId,unitId);boolean privileged=hasPrivilegedOpinionScope(organizationId,unitId);
        return service.opinions(TENANT,matterId,contractId).stream()
                .filter(value->within(value.metadata(),organizationId,unitId))
                .filter(value->!value.privileged()||privileged).toList();
    }

    @PostMapping("/legal/opinions")
    public ContractLegalDirectory.LegalOpinion requestOpinion(@RequestBody OpinionRequest body) {
        require("CREATE","legal/opinions",body.organizationId(),body.unitId());
        if(body.privileged())require("PRIVILEGED","legal/opinions",body.organizationId(),body.unitId());
        if(body.contractId()!=null)requireOwned(contract(body.contractId()).references(),body.organizationId(),body.unitId());
        if(body.matterId()!=null)requireOwned(matter(body.matterId()).metadata(),body.organizationId(),body.unitId());
        return service.requestOpinion(new ContractLegalDirectory.LegalOpinion(body.id(),TENANT,body.reference(),
                body.matterId(),body.contractId(),body.title(),actor(),null,null,ContractLegalDirectory.OpinionStatus.REQUESTED,
                Instant.now(),null,null,body.privileged(),scopeMetadata(body.metadata(),body.organizationId(),body.unitId())));
    }

    @PostMapping("/legal/opinions/{id}/issue")
    public ContractLegalDirectory.LegalOpinion issueOpinion(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestBody IssueOpinionRequest body) {
        require("APPROVE","legal/opinions",organizationId,unitId);
        var opinion=service.directory().find(TENANT,id,ContractLegalDirectory.LegalOpinion.class)
                .orElseThrow(()->new IllegalArgumentException("legal opinion does not exist"));
        requireOwned(opinion.metadata(),organizationId,unitId);
        if(opinion.privileged())require("PRIVILEGED","legal/opinions",organizationId,unitId);
        return service.issueOpinion(TENANT,id,actor(),body.conclusion(),body.documentReference());
    }

    @PostMapping("/legal/matters")
    public ContractLegalDirectory.LegalMatter createMatter(@RequestBody MatterRequest body) {
        require("CREATE","legal/matters",body.organizationId(),body.unitId());
        if(body.contractId()!=null)requireOwned(contract(body.contractId()).references(),body.organizationId(),body.unitId());
        if(body.lawFirmId()!=null){var firm=service.directory().find(TENANT,body.lawFirmId(),ContractLegalDirectory.LawFirm.class).orElseThrow(()->new IllegalArgumentException("law firm does not exist"));requireOwned(firm.metadata(),body.organizationId(),body.unitId());}
        Map<String,String> metadata=new HashMap<>(body.metadata()==null?Map.of():body.metadata());
        metadata.put("organizationId",Long.toString(body.organizationId()));
        if(body.unitId()==null)metadata.remove("unitId");else metadata.put("unitId",Long.toString(body.unitId()));
        return service.saveMatter(new ContractLegalDirectory.LegalMatter(body.id(),TENANT,body.reference(),body.type(),
                body.caseNumber(),body.title(),body.court(),body.jurisdiction(),ContractLegalDirectory.LegalMatterStatus.OPEN,
                body.contractId(),body.lawFirmId(),body.leadCounsel(),body.filedOn(),null,null,metadata));
    }

    @PostMapping("/legal/matters/{id}/deadlines")
    public ContractLegalDirectory.LegalDeadline addDeadline(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestBody DeadlineRequest body) {
        require("UPDATE","legal/deadlines",organizationId,unitId);requireOwned(matter(id).metadata(),organizationId,unitId);
        return service.saveDeadline(new ContractLegalDirectory.LegalDeadline(body.id(),TENANT,id,body.code(),body.kind(),
                body.description(),body.dueOn(),body.responsibleParty(),ContractLegalDirectory.DeadlineStatus.OPEN,
                null,null,body.metadata()));
    }

    @GetMapping("/legal/matters/{id}/deadlines")
    public List<ContractLegalDirectory.LegalDeadline> deadlines(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId) {
        require("READ","legal/deadlines",organizationId,unitId);requireOwned(matter(id).metadata(),organizationId,unitId);
        return service.deadlines(TENANT,id);
    }

    @PostMapping("/legal/deadlines/{id}/complete")
    public ContractLegalDirectory.LegalDeadline completeDeadline(@PathVariable String id,@RequestParam long organizationId,
            @RequestParam(required=false) Long unitId,@RequestBody Evidence body) {
        require("APPROVE","legal/deadlines",organizationId,unitId);
        var deadline=service.directory().find(TENANT,id,ContractLegalDirectory.LegalDeadline.class)
                .orElseThrow(()->new IllegalArgumentException("legal deadline does not exist"));
        requireOwned(matter(deadline.matterId()).metadata(),organizationId,unitId);
        return service.completeDeadline(TENANT,id,body.reference());
    }

    private Contract contract(String id){return service.findContract(TENANT,id).orElseThrow(()->new IllegalArgumentException("contract does not exist"));}
    private ContractLegalDirectory.LegalMatter matter(String id){return service.directory().find(TENANT,id,ContractLegalDirectory.LegalMatter.class).orElseThrow(()->new IllegalArgumentException("legal matter does not exist"));}
    private boolean hasPrivilegedOpinionScope(long organizationId,Long unitId){try{require("PRIVILEGED","legal/opinions",organizationId,unitId);return true;}catch(org.springframework.security.access.AccessDeniedException denied){return false;}}
    private Map<String,String> scopeMetadata(Map<String,String> source,long organizationId,Long unitId){Map<String,String> result=new HashMap<>(source==null?Map.of():source);result.put("organizationId",Long.toString(organizationId));if(unitId==null)result.remove("unitId");else result.put("unitId",Long.toString(unitId));return result;}
    private void require(String action,long organizationId,Long unitId){require(action,"contracts",organizationId,unitId);}
    private void require(String action,String resource,long organizationId,Long unitId){if(organizationId<=0||unitId!=null&&unitId<=0)throw new IllegalArgumentException("organization and unit IDs must be positive");access.requireScope(resource,action,organizationId,unitId);}
    private void requireOwned(Map<String,String> values,long organizationId,Long unitId){if(!within(values,organizationId,unitId))throw new org.springframework.security.access.AccessDeniedException("Contract/legal record is outside the selected organization and unit.");}
    private boolean within(Map<String,String> values,long organizationId,Long unitId){return Long.toString(organizationId).equals(values.get("organizationId"))&&Objects.equals(unitId==null?null:Long.toString(unitId),values.get("unitId"));}
    private String actor(){var current=actors.currentActor();if(current==null||!current.authenticated()||current.id()==null||current.id().isBlank()||current.id().equalsIgnoreCase("anonymous"))throw new org.springframework.security.access.AccessDeniedException("An authenticated actor is required.");return current.id();}

    public record CreateContract(String id,long organizationId,Long unitId,String counterpartyId,String number,String subject,
            LocalDate startsAt,LocalDate endsAt,BigDecimal value,String currency,ContractLegalDirectory.PartyRole partyRole,
            Map<String,String> references){}
    public record AmendmentRequest(String summary,LocalDate effectiveOn,LocalDate newEndDate,BigDecimal newValue,
            String currency,Map<String,String> metadata){}
    public record Decision(boolean approve,String reason){}
    public record AdjustmentRequest(String indexCode,BigDecimal ratePercent,LocalDate effectiveOn,Map<String,String> metadata){}
    public record RenewalRequest(LocalDate startOn,LocalDate endOn,Map<String,String> metadata){}
    public record ObligationRequest(String id,String code,String description,LocalDate dueOn,String responsibleParty,String notes,Map<String,String> metadata){}
    public record ObligationUpdate(ContractLegalDirectory.ObligationStatus status,String evidenceReference,String notes){}
    public record RiskRequest(String id,String reference,String category,String description,int likelihood,int impact,
            BigDecimal exposureAmount,String currency,String mitigation,String owner,
            ContractLegalDirectory.ContractRiskStatus status,Map<String,String> metadata){}
    public record MatterRequest(String id,long organizationId,Long unitId,String reference,
            ContractLegalDirectory.LegalMatterType type,String caseNumber,String title,String court,String jurisdiction,
            String contractId,String lawFirmId,String leadCounsel,LocalDate filedOn,Map<String,String> metadata){}
    public record DeadlineRequest(String id,String code,ContractLegalDirectory.DeadlineKind kind,String description,
            LocalDate dueOn,String responsibleParty,Map<String,String> metadata){}
    public record Evidence(String reference){}
    public record CloseMatterRequest(LocalDate closedOn,String outcome){}
    public record LawFirmRequest(String id,long organizationId,Long unitId,String code,String name,String registrationNumber,String email,String phone,boolean active,Map<String,String> metadata){}
    public record PowerOfAttorneyRequest(String id,long organizationId,Long unitId,String reference,String principalId,String attorneyName,String attorneyRegistration,String scope,LocalDate issuedOn,LocalDate expiresOn,String documentReference,Map<String,String> metadata){}
    public record RevokeAuthorityRequest(String reason){}
    public record OpinionRequest(String id,long organizationId,Long unitId,String reference,String matterId,String contractId,String title,boolean privileged,Map<String,String> metadata){}
    public record IssueOpinionRequest(String conclusion,String documentReference){}
}
