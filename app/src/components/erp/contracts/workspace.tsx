"use client";

import * as React from "react";
import axios from "axios";
import { Layout } from "components/layout";
import { Message } from "components/common/message";
import { useSession } from "components/auth/session-provider";
import { ReferenceField } from "components/erp/shared/record-workspace";
import { createErpService } from "api/services/erp.service";
import { contractsService } from "api/services/contracts.service";
import type { ErpField, ErpValue } from "api/models/erp";
import type { ContractAdjustment, ContractAmendment, ContractObligation, ContractRecord, ContractRenewal, ContractRisk, LawFirmRecord, LegalDeadlineRecord, LegalMatterRecord, LegalOpinionRecord, PowerOfAttorneyRecord } from "api/models/erp/contracts";
import styles from "components/erp/shared/workspace.module.css";

const core = createErpService("core");
const reference = (name: string, label: string, resource: string, required = true): ErpField =>
    ({ name, label, type: "reference", required, reference: resource, choices: [] });
const organizationField = reference("organizationId", "Organization", "organizations");
const unitField = reference("unitId", "Unit", "units", false);
const counterpartyField = reference("counterpartyId", "Supplier or party", "people", false);
const failure = (error: unknown) => axios.isAxiosError(error) && typeof error.response?.data?.detail === "string"
    ? error.response.data.detail : "Unable to complete the request. Check your access and the API connection.";
const riskSeverity = (score: number) => score >= 20 ? "CRITICAL" : score >= 12 ? "HIGH" : score >= 6 ? "MODERATE" : "LOW";

export function ContractLegalWorkspace() {
    return <Layout title="Contracts and legal"><ContractLegalContent /></Layout>;
}

function ContractLegalContent() {
    const { can } = useSession();
    const [organization, setOrganization] = React.useState<ErpValue>(null);
    const [unit, setUnit] = React.useState<ErpValue>(null);
    const [counterparty, setCounterparty] = React.useState<ErpValue>(null);
    const [contracts, setContracts] = React.useState<ContractRecord[]>([]);
    const [matters, setMatters] = React.useState<LegalMatterRecord[]>([]);
    const [selectedMatter, setSelectedMatter] = React.useState<LegalMatterRecord | null>(null);
    const [deadlines, setDeadlines] = React.useState<LegalDeadlineRecord[]>([]);
    const [lawFirms, setLawFirms] = React.useState<LawFirmRecord[]>([]);
    const [authorities, setAuthorities] = React.useState<PowerOfAttorneyRecord[]>([]);
    const [opinions, setOpinions] = React.useState<LegalOpinionRecord[]>([]);
    const [selected, setSelected] = React.useState<ContractRecord | null>(null);
    const [amendments, setAmendments] = React.useState<ContractAmendment[]>([]);
    const [adjustments, setAdjustments] = React.useState<ContractAdjustment[]>([]);
    const [risks, setRisks] = React.useState<ContractRisk[]>([]);
    const [renewals, setRenewals] = React.useState<ContractRenewal[]>([]);
    const [obligations, setObligations] = React.useState<ContractObligation[]>([]);
    const [detailRevision, setDetailRevision] = React.useState(0);
    const [error, setError] = React.useState("");
    const [busy, setBusy] = React.useState(false);
    const [number, setNumber] = React.useState("");
    const [subject, setSubject] = React.useState("");
    const [partyRole, setPartyRole] = React.useState<"CUSTOMER" | "SUPPLIER" | "SERVICE_PROVIDER" | "PARTNER">("SUPPLIER");
    const [contractValue, setContractValue] = React.useState("");
    const [contractCurrency, setContractCurrency] = React.useState("BRL");
    const [startsAt, setStartsAt] = React.useState("");
    const [endsAt, setEndsAt] = React.useState("");
    const [amendmentSummary, setAmendmentSummary] = React.useState("");
    const [amendmentDate, setAmendmentDate] = React.useState("");
    const [amendmentNewEndDate, setAmendmentNewEndDate] = React.useState("");
    const [amendmentNewValue, setAmendmentNewValue] = React.useState("");
    const [adjustmentIndex, setAdjustmentIndex] = React.useState("");
    const [adjustmentRate, setAdjustmentRate] = React.useState("");
    const [adjustmentDate, setAdjustmentDate] = React.useState("");
    const [riskReference, setRiskReference] = React.useState("");
    const [riskDescription, setRiskDescription] = React.useState("");
    const [riskLikelihood, setRiskLikelihood] = React.useState("3");
    const [riskImpact, setRiskImpact] = React.useState("3");
    const [renewalStart, setRenewalStart] = React.useState("");
    const [renewalEnd, setRenewalEnd] = React.useState("");
    const [obligationCode, setObligationCode] = React.useState("");
    const [obligationDescription, setObligationDescription] = React.useState("");
    const [obligationDueOn, setObligationDueOn] = React.useState("");
    const [obligationEvidence, setObligationEvidence] = React.useState("");
    const [matterReference, setMatterReference] = React.useState("");
    const [matterTitle, setMatterTitle] = React.useState("");
    const [matterType, setMatterType] = React.useState<"JUDICIAL" | "ADMINISTRATIVE">("ADMINISTRATIVE");
    const [matterLawFirmId, setMatterLawFirmId] = React.useState("");
    const [deadlineCode, setDeadlineCode] = React.useState("");
    const [deadlineDescription, setDeadlineDescription] = React.useState("");
    const [deadlineDate, setDeadlineDate] = React.useState("");
    const [deadlineEvidence, setDeadlineEvidence] = React.useState("");
    const [firmCode, setFirmCode] = React.useState("");
    const [firmName, setFirmName] = React.useState("");
    const [firmEmail, setFirmEmail] = React.useState("");
    const [authorityReference, setAuthorityReference] = React.useState("");
    const [authorityPrincipal, setAuthorityPrincipal] = React.useState("");
    const [authorityAttorney, setAuthorityAttorney] = React.useState("");
    const [authorityScope, setAuthorityScope] = React.useState("");
    const [authorityIssuedOn, setAuthorityIssuedOn] = React.useState("");
    const [authorityExpiresOn, setAuthorityExpiresOn] = React.useState("");
    const [opinionReference, setOpinionReference] = React.useState("");
    const [opinionTitle, setOpinionTitle] = React.useState("");
    const [opinionConclusion, setOpinionConclusion] = React.useState("");
    const [opinionPrivileged, setOpinionPrivileged] = React.useState(false);
    const [matterClosedOn, setMatterClosedOn] = React.useState("");
    const [matterOutcome, setMatterOutcome] = React.useState("");
    const organizationId = organization ? Number(organization) : undefined;
    const unitId = unit ? Number(unit) : undefined;
    const canRead = can("contracts", "READ", organizationId, unitId);
    const canCreate = can("contracts", "CREATE", organizationId, unitId);
    const canUpdate = can("contracts", "UPDATE", organizationId, unitId);
    const canApprove = can("contracts", "APPROVE", organizationId, unitId);
    const canFinance = can("contracts", "FINANCE", organizationId, unitId);
    const canReadLegal = can("legal/matters", "READ", organizationId, unitId);
    const canCreateLegal = can("legal/matters", "CREATE", organizationId, unitId);
    const canApproveMatter = can("legal/matters", "APPROVE", organizationId, unitId);
    const canUpdateLegal = can("legal/deadlines", "UPDATE", organizationId, unitId);
    const canApproveLegal = can("legal/deadlines", "APPROVE", organizationId, unitId);
    const canReadAuthorities = can("legal/authorities", "READ", organizationId, unitId);
    const canCreateAuthorities = can("legal/authorities", "CREATE", organizationId, unitId);
    const canApproveAuthorities = can("legal/authorities", "APPROVE", organizationId, unitId);
    const canReadOpinions = can("legal/opinions", "READ", organizationId, unitId);
    const canCreateOpinions = can("legal/opinions", "CREATE", organizationId, unitId);
    const canApproveOpinions = can("legal/opinions", "APPROVE", organizationId, unitId);
    const canReadPrivilegedOpinions = can("legal/opinions", "PRIVILEGED", organizationId, unitId);

    const refresh = React.useCallback(async () => {
        if (!organizationId) { setContracts([]); setMatters([]); return; }
        try {
            const [nextContracts, nextMatters] = await Promise.all([
                canRead ? contractsService.list(organizationId, unitId) : Promise.resolve([]),
                canReadLegal ? contractsService.matters(organizationId, unitId) : Promise.resolve([])
            ]);
            setContracts(nextContracts); setMatters(nextMatters); setError("");
        } catch (reason) { setError(failure(reason)); }
    }, [organizationId, unitId, canRead, canReadLegal]);
    React.useEffect(() => {
        if (!organizationId) return;
        let active = true;
        Promise.all([
            canRead ? contractsService.list(organizationId, unitId) : Promise.resolve([]),
            canReadLegal ? contractsService.matters(organizationId, unitId) : Promise.resolve([])
        ]).then(([nextContracts, nextMatters]) => {
            if (active) { setContracts(nextContracts); setMatters(nextMatters); setError(""); }
        }).catch(reason => { if (active) setError(failure(reason)); });
        return () => { active = false; };
    }, [organizationId, unitId, canRead, canReadLegal]);
    React.useEffect(() => {
        if (!organizationId) return;
        let active = true;
        Promise.all([
            canReadLegal ? contractsService.lawFirms(organizationId, unitId) : Promise.resolve([]),
            canReadAuthorities ? contractsService.powersOfAttorney(organizationId, unitId) : Promise.resolve([]),
            canReadOpinions ? contractsService.opinions(organizationId, unitId) : Promise.resolve([])
        ]).then(([firms, nextAuthorities, nextOpinions]) => {
            if (active) { setLawFirms(firms); setAuthorities(nextAuthorities); setOpinions(nextOpinions); }
        }).catch(reason => { if (active) setError(failure(reason)); });
        return () => { active = false; };
    }, [organizationId, unitId, canReadLegal, canReadAuthorities, canReadOpinions]);
    React.useEffect(() => {
        if (!selected || !organizationId) return;
        let active = true;
        Promise.all([
            contractsService.amendments(selected.id.value, organizationId, unitId),
            contractsService.adjustments(selected.id.value, organizationId, unitId),
            contractsService.risks(selected.id.value, organizationId, unitId),
            contractsService.renewals(selected.id.value, organizationId, unitId),
            contractsService.obligations(selected.id.value, organizationId, unitId)
        ]).then(([nextAmendments, nextAdjustments, nextRisks, nextRenewals, nextObligations]) => {
            if (active) { setAmendments(nextAmendments); setAdjustments(nextAdjustments); setRisks(nextRisks); setRenewals(nextRenewals); setObligations(nextObligations); }
        }).catch(reason => { if (active) setError(failure(reason)); });
        return () => { active = false; };
    }, [selected, organizationId, unitId, detailRevision]);

    async function run(action: () => Promise<unknown>, reload = true) {
        if (busy) return;
        setBusy(true); setError("");
        try { await action(); setDetailRevision(value => value + 1); if (reload) await refresh(); }
        catch (reason) { setError(failure(reason)); }
        finally { setBusy(false); }
    }

    async function createContract(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!organizationId || !number.trim() || !subject.trim() || !startsAt || !canCreate) return;
        await run(async () => {
            const saved = await contractsService.create({
                id: crypto.randomUUID(), organizationId, unitId,
                ...(counterparty ? { counterpartyId: String(counterparty) } : {}),
                number: number.trim(), subject: subject.trim(), startsAt,
                ...(endsAt ? { endsAt } : {}), partyRole,
                ...(contractValue ? { value: contractValue, currency: contractCurrency } : {})
            });
            setSelected(saved); setNumber(""); setSubject(""); setStartsAt(""); setEndsAt(""); setContractValue("");
        });
    }

    async function createMatter(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!organizationId || !matterReference.trim() || !matterTitle.trim() || !canCreateLegal) return;
        await run(async () => {
            const matter = await contractsService.createMatter({ id: crypto.randomUUID(), organizationId, unitId,
                reference: matterReference.trim(), type: matterType, title: matterTitle.trim(), ...(matterLawFirmId ? { lawFirmId: matterLawFirmId } : {}) });
            setSelectedMatter(matter); setDeadlines([]);
            setMatterReference(""); setMatterTitle(""); setMatterLawFirmId("");
        });
    }

    async function openMatter(matter: LegalMatterRecord) {
        setSelectedMatter(matter); setDeadlines([]); setError("");
        if (!organizationId || !canReadLegal) return;
        try { setDeadlines(await contractsService.deadlines(matter.id, organizationId, unitId)); }
        catch (reason) { setError(failure(reason)); }
    }

    async function createLawFirm(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!organizationId || !firmCode.trim() || !firmName.trim() || !firmEmail.trim()) return;
        await run(async () => { await contractsService.createLawFirm({ id: crypto.randomUUID(), organizationId, unitId, code: firmCode.trim(), name: firmName.trim(), email: firmEmail.trim() }); setFirmCode(""); setFirmName(""); setFirmEmail(""); });
    }

    async function createAuthority(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!organizationId || !authorityReference.trim() || !authorityPrincipal.trim() || !authorityAttorney.trim() || !authorityScope.trim() || !authorityIssuedOn) return;
        await run(async () => { await contractsService.createPowerOfAttorney({ id: crypto.randomUUID(), organizationId, unitId, reference: authorityReference.trim(), principalId: authorityPrincipal.trim(), attorneyName: authorityAttorney.trim(), scope: authorityScope.trim(), issuedOn: authorityIssuedOn, ...(authorityExpiresOn ? { expiresOn: authorityExpiresOn } : {}) }); setAuthorityReference(""); setAuthorityPrincipal(""); setAuthorityAttorney(""); setAuthorityScope(""); setAuthorityIssuedOn(""); setAuthorityExpiresOn(""); });
    }

    async function createOpinion(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!organizationId || !selectedMatter || !opinionReference.trim() || !opinionTitle.trim() || !canCreateOpinions) return;
        await run(async () => { await contractsService.requestOpinion({ id: crypto.randomUUID(), organizationId, unitId, reference: opinionReference.trim(), matterId: selectedMatter.id, title: opinionTitle.trim(), privileged: opinionPrivileged }); setOpinionReference(""); setOpinionTitle(""); setOpinionPrivileged(false); });
    }

    async function closeMatter(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!organizationId || !selectedMatter || !matterClosedOn || !matterOutcome.trim() || !canApproveMatter) return;
        await run(async () => { const closed = await contractsService.closeMatter(selectedMatter.id, organizationId, unitId, { closedOn: matterClosedOn, outcome: matterOutcome.trim() }); setSelectedMatter(closed); setMatterClosedOn(""); setMatterOutcome(""); });
    }

    return <div className={styles.workspace}>
        <p className={styles.intro}>Manage supplier contracts, approval and effective dates, contract risks, and legal matters within your organization scope.</p>
        {error && <Message type="error" text={error} onClose={() => setError("")} />}
        <div className={styles.fields}>
            <ReferenceField service={core} field={organizationField} value={organization} organizationId={null}
                onChange={value => { setOrganization(value); setUnit(null); setSelected(null); }} />
            <ReferenceField service={core} field={unitField} value={unit} organizationId={organization}
                onChange={value => setUnit(value)} />
        </div>
        {!organizationId && <Message type="info" text="Choose an organization to view and create contracts." />}

        {organizationId && canCreate && <section>
            <h2>New contract</h2>
            <form className={styles.fields} onSubmit={createContract}>
                <label>Contract number<input required value={number} onChange={event => setNumber(event.target.value)} /></label>
                <label>Subject<input required value={subject} onChange={event => setSubject(event.target.value)} /></label>
                <label>Party role<select value={partyRole} onChange={event => setPartyRole(event.target.value as typeof partyRole)}><option value="CUSTOMER">Customer</option><option value="SUPPLIER">Supplier</option><option value="SERVICE_PROVIDER">Service provider</option><option value="PARTNER">Partner</option></select></label>
                <ReferenceField service={core} field={counterpartyField} value={counterparty} organizationId={organization}
                    onChange={value => setCounterparty(value)} />
                <label>Start date<input type="date" required value={startsAt} onChange={event => setStartsAt(event.target.value)} /></label>
                <label>End date<input type="date" value={endsAt} onChange={event => setEndsAt(event.target.value)} /></label>
                <label>Contract value<input type="number" min="0" step="0.01" value={contractValue} onChange={event => setContractValue(event.target.value)} /></label>
                <label>Currency<input maxLength={3} value={contractCurrency} onChange={event => setContractCurrency(event.target.value.toUpperCase())} /></label>
                <button type="submit" disabled={busy}>Save draft</button>
            </form>
        </section>}

        <section>
            <h2>Contracts</h2>
            {organizationId && !canRead && <Message type="warn" text="Your profile cannot read contracts in this scope." />}
            <div className={styles.tableContainer}><table><thead><tr><th>Number</th><th>Subject</th><th>Status</th><th>Term</th><th>Actions</th></tr></thead>
                <tbody>{contracts.map(contract => <tr key={contract.id.value}>
                    <td>{contract.number}</td><td>{contract.subject}</td><td>{contract.status}</td>
                    <td>{contract.startsAt}{contract.endsAt ? ` – ${contract.endsAt}` : " – open"}</td>
                    <td><button type="button" onClick={() => setSelected(contract)}>Details</button>{contract.status === "DRAFT" && canApprove &&
                        <button type="button" disabled={busy} onClick={() => void run(async () => { await contractsService.activate(contract.id.value, organizationId!, unitId); setSelected(null); })}>Approve and activate</button>}</td>
                </tr>)}{contracts.length === 0 && <tr><td colSpan={5}>No contracts in this scope.</td></tr>}</tbody></table></div>
        </section>

        {selected && organizationId && <section>
            <h2>Contract {selected.number}: {selected.subject}</h2>
            {canUpdate && <>
                <form className={styles.fields} onSubmit={event => { event.preventDefault(); if (!amendmentSummary.trim() || !amendmentDate) return; void run(async () => { await contractsService.requestAmendment(selected.id.value, organizationId, unitId, { summary: amendmentSummary.trim(), effectiveOn: amendmentDate, ...(amendmentNewEndDate ? { newEndDate: amendmentNewEndDate } : {}), ...(amendmentNewValue ? { newValue: amendmentNewValue, currency: selected.value?.currency.currencyCode ?? "BRL" } : {}) }); setAmendmentSummary(""); setAmendmentDate(""); setAmendmentNewEndDate(""); setAmendmentNewValue(""); }); }}>
                    <label>Amendment summary<input value={amendmentSummary} onChange={event => setAmendmentSummary(event.target.value)} /></label>
                    <label>Effective date<input type="date" value={amendmentDate} onChange={event => setAmendmentDate(event.target.value)} /></label>
                    <label>New contract end date<input type="date" value={amendmentNewEndDate} onChange={event => setAmendmentNewEndDate(event.target.value)} /></label>
                    <label>New contract value<input type="number" min="0" step="0.01" value={amendmentNewValue} onChange={event => setAmendmentNewValue(event.target.value)} /></label>
                    <button type="submit" disabled={busy}>Request amendment</button>
                </form>
                <form className={styles.fields} onSubmit={event => { event.preventDefault(); if (!riskReference.trim() || !riskDescription.trim()) return; void run(async () => { await contractsService.assessRisk(selected.id.value, organizationId, unitId, { id: crypto.randomUUID(), reference: riskReference.trim(), category: "OPERATIONAL", description: riskDescription.trim(), likelihood: Number(riskLikelihood), impact: Number(riskImpact) }); setRiskReference(""); setRiskDescription(""); }); }}>
                    <label>Risk reference<input value={riskReference} onChange={event => setRiskReference(event.target.value)} /></label>
                    <label>Risk description<input value={riskDescription} onChange={event => setRiskDescription(event.target.value)} /></label>
                    <label>Likelihood (1–5)<input type="number" min="1" max="5" value={riskLikelihood} onChange={event => setRiskLikelihood(event.target.value)} /></label>
                    <label>Impact (1–5)<input type="number" min="1" max="5" value={riskImpact} onChange={event => setRiskImpact(event.target.value)} /></label>
                    <button type="submit" disabled={busy}>Record risk</button>
                </form>
                <h3>Amendment history</h3>
                <ul>{amendments.map(amendment => <li key={amendment.id}>{amendment.sequence}. {amendment.summary} — {amendment.status}, effective {amendment.effectiveOn}
                    {amendment.status === "DRAFT" && canApprove && <button type="button" disabled={busy} onClick={() => void run(() => contractsService.decideAmendment(amendment.id, organizationId, unitId, true))}>Approve</button>}</li>)}
                    {amendments.length === 0 && <li>No amendments recorded.</li>}</ul>
                <h3>Indexed adjustments</h3>
                {canFinance && selected.value && <form className={styles.fields} onSubmit={event => { event.preventDefault(); if (!adjustmentIndex.trim() || !adjustmentRate || !adjustmentDate) return; void run(async () => { await contractsService.requestAdjustment(selected.id.value, organizationId, unitId, { indexCode: adjustmentIndex.trim(), ratePercent: adjustmentRate, effectiveOn: adjustmentDate }); setAdjustmentIndex(""); setAdjustmentRate(""); setAdjustmentDate(""); }); }}>
                    <label>Index code<input required value={adjustmentIndex} onChange={event => setAdjustmentIndex(event.target.value)} /></label>
                    <label>Rate percent<input type="number" min="-100" max="1000" step="0.0001" required value={adjustmentRate} onChange={event => setAdjustmentRate(event.target.value)} /></label>
                    <label>Effective date<input type="date" required value={adjustmentDate} onChange={event => setAdjustmentDate(event.target.value)} /></label>
                    <button type="submit" disabled={busy}>Request adjustment</button>
                </form>}
                <ul>{adjustments.map(adjustment => <li key={adjustment.id}>{adjustment.indexCode} {adjustment.ratePercent}%: {adjustment.baseAmount} → {adjustment.adjustedAmount} {adjustment.currency}, effective {adjustment.effectiveOn} — {adjustment.status}
                    {adjustment.status === "REQUESTED" && canFinance && <button type="button" disabled={busy} onClick={() => void run(() => contractsService.decideAdjustment(adjustment.id, organizationId, unitId, true))}>Approve</button>}
                    {adjustment.status === "APPROVED" && canFinance && new Date(`${adjustment.effectiveOn}T00:00:00`) <= new Date() && <button type="button" disabled={busy} onClick={() => void run(() => contractsService.applyDueAdjustments(selected.id.value, organizationId, unitId))}>Apply due adjustments</button>}</li>)}
                    {adjustments.length === 0 && <li>No indexed adjustments recorded.</li>}</ul>
                <h3>Risk register</h3>
                <ul>{risks.map(risk => { const score = risk.likelihood * risk.impact; return <li key={risk.id}>{risk.reference}: {risk.description} — {riskSeverity(score)} ({score}/25)</li>; })}
                    {risks.length === 0 && <li>No risks assessed.</li>}</ul>
                <h3>Renewals</h3>
                <form className={styles.fields} onSubmit={event => { event.preventDefault(); if (!renewalStart || !renewalEnd) return; void run(async () => { await contractsService.requestRenewal(selected.id.value, organizationId, unitId, { startOn: renewalStart, endOn: renewalEnd }); setRenewalStart(""); setRenewalEnd(""); }); }}>
                    <label>Renewal starts<input type="date" required value={renewalStart} onChange={event => setRenewalStart(event.target.value)} /></label>
                    <label>Renewal ends<input type="date" required value={renewalEnd} onChange={event => setRenewalEnd(event.target.value)} /></label>
                    <button type="submit" disabled={busy || !canUpdate}>Request renewal</button>
                </form>
                <ul>{renewals.map(renewal => <li key={renewal.id}>{renewal.requestedStartOn} to {renewal.requestedEndOn} — {renewal.status}
                    {renewal.status === "REQUESTED" && canApprove && <button type="button" disabled={busy} onClick={() => void run(() => contractsService.decideRenewal(renewal.id, organizationId, unitId, true))}>Approve</button>}
                    {renewal.status === "APPROVED" && canApprove && <button type="button" disabled={busy} onClick={() => void run(() => contractsService.completeRenewal(renewal.id, organizationId, unitId))}>Apply when effective</button>}</li>)}
                    {renewals.length === 0 && <li>No renewals recorded.</li>}</ul>
                <h3>Obligations</h3>
                <form className={styles.fields} onSubmit={event => { event.preventDefault(); if (!obligationCode.trim() || !obligationDescription.trim() || !obligationDueOn) return; void run(async () => { await contractsService.saveObligation(selected.id.value, organizationId, unitId, { id: crypto.randomUUID(), code: obligationCode.trim(), description: obligationDescription.trim(), dueOn: obligationDueOn }); setObligationCode(""); setObligationDescription(""); setObligationDueOn(""); }); }}>
                    <label>Code<input required value={obligationCode} onChange={event => setObligationCode(event.target.value)} /></label>
                    <label>Description<input required value={obligationDescription} onChange={event => setObligationDescription(event.target.value)} /></label>
                    <label>Due date<input type="date" required value={obligationDueOn} onChange={event => setObligationDueOn(event.target.value)} /></label>
                    <button type="submit" disabled={busy || !canUpdate}>Add obligation</button>
                </form>
                <label>Evidence reference for fulfillment<input value={obligationEvidence} onChange={event => setObligationEvidence(event.target.value)} /></label>
                <ul>{obligations.map(obligation => <li key={obligation.id}>{obligation.code}: {obligation.description} — due {obligation.dueOn}, {obligation.status}
                    {obligation.status !== "FULFILLED" && obligation.status !== "WAIVED" && canUpdate && <button type="button" disabled={busy || !obligationEvidence.trim()} onClick={() => void run(async () => { await contractsService.updateObligation(obligation.id, organizationId, unitId, { status: "FULFILLED", evidenceReference: obligationEvidence.trim() }); setObligationEvidence(""); })}>Mark fulfilled</button>}</li>)}
                    {obligations.length === 0 && <li>No obligations recorded.</li>}</ul>
            </>}
            <button type="button" onClick={() => setSelected(null)}>Close details</button>
        </section>}

        {organizationId && (canCreateLegal || canReadLegal) && <section>
            <h2>Legal matters</h2>
            {canCreateLegal && <form className={styles.fields} onSubmit={createMatter}>
                <label>Reference<input value={matterReference} onChange={event => setMatterReference(event.target.value)} /></label>
                <label>Title<input value={matterTitle} onChange={event => setMatterTitle(event.target.value)} /></label>
                <label>Type<select value={matterType} onChange={event => setMatterType(event.target.value as "JUDICIAL" | "ADMINISTRATIVE")}><option value="ADMINISTRATIVE">Administrative</option><option value="JUDICIAL">Judicial</option></select></label>
                <label>Law firm<select value={matterLawFirmId} onChange={event => setMatterLawFirmId(event.target.value)}><option value="">No firm assigned</option>{lawFirms.filter(firm => firm.active).map(firm => <option key={firm.id} value={firm.id}>{firm.name}</option>)}</select></label>
                <button type="submit" disabled={busy}>Register matter</button>
            </form>}
            <ul>{matters.map(matter => <li key={matter.id}><button type="button" onClick={() => void openMatter(matter)}>{matter.reference} — {matter.title} ({matter.status})</button></li>)}</ul>
            {selectedMatter && <>
                <h3>Deadlines for {selectedMatter.reference}</h3>
                {selectedMatter.status !== "CLOSED" && canApproveMatter && <form className={styles.fields} onSubmit={closeMatter}>
                    <label>Closed on<input type="date" required value={matterClosedOn} onChange={event => setMatterClosedOn(event.target.value)} /></label>
                    <label>Outcome<input required value={matterOutcome} onChange={event => setMatterOutcome(event.target.value)} /></label>
                    <button type="submit" disabled={busy}>Close legal matter</button>
                </form>}
                {canUpdateLegal && <form className={styles.fields} onSubmit={event => { event.preventDefault(); if (!deadlineCode.trim() || !deadlineDescription.trim() || !deadlineDate) return; void run(async () => { await contractsService.addDeadline(selectedMatter.id, organizationId, unitId, { id: crypto.randomUUID(), code: deadlineCode.trim(), kind: "OTHER", description: deadlineDescription.trim(), dueOn: deadlineDate }); setDeadlineCode(""); setDeadlineDescription(""); setDeadlineDate(""); setDeadlines(await contractsService.deadlines(selectedMatter.id, organizationId, unitId)); }, false); }}>
                    <label>Deadline code<input required value={deadlineCode} onChange={event => setDeadlineCode(event.target.value)} /></label>
                    <label>Description<input required value={deadlineDescription} onChange={event => setDeadlineDescription(event.target.value)} /></label>
                    <label>Due date<input type="date" required value={deadlineDate} onChange={event => setDeadlineDate(event.target.value)} /></label>
                    <button type="submit" disabled={busy}>Add deadline</button>
                </form>}
                {canApproveLegal && <label>Evidence reference for completion<input value={deadlineEvidence} onChange={event => setDeadlineEvidence(event.target.value)} /></label>}
                <ul>{deadlines.map(deadline => <li key={deadline.id}>{deadline.code}: {deadline.description} — due {deadline.dueOn}, {deadline.status}
                    {deadline.status === "OPEN" && canApproveLegal && <button type="button" disabled={busy || !deadlineEvidence.trim()} onClick={() => void run(async () => { await contractsService.completeDeadline(deadline.id, organizationId, unitId, deadlineEvidence.trim()); setDeadlineEvidence(""); setDeadlines(await contractsService.deadlines(selectedMatter.id, organizationId, unitId)); }, false)}>Complete</button>}</li>)}
                    {deadlines.length === 0 && <li>No deadlines recorded.</li>}</ul>
                {canCreateOpinions && <form className={styles.fields} onSubmit={createOpinion}>
                    <label>Opinion reference<input required value={opinionReference} onChange={event => setOpinionReference(event.target.value)} /></label>
                    <label>Question/title<input required value={opinionTitle} onChange={event => setOpinionTitle(event.target.value)} /></label>
                    {canReadPrivilegedOpinions && <label><input type="checkbox" checked={opinionPrivileged} onChange={event => setOpinionPrivileged(event.target.checked)} /> Privileged opinion</label>}
                    <button type="submit" disabled={busy}>Request legal opinion</button>
                </form>}
            </>}
            {canReadOpinions && <><h3>Legal opinions</h3><ul>{opinions.map(opinion => <li key={opinion.id}>{opinion.reference}: {opinion.title} — {opinion.status}{opinion.privileged ? " (privileged)" : ""}
                {opinion.status === "REQUESTED" && canApproveOpinions && (!opinion.privileged || canReadPrivilegedOpinions) && <><input aria-label={`Conclusion for ${opinion.reference}`} value={opinionConclusion} onChange={event => setOpinionConclusion(event.target.value)} /><button type="button" disabled={busy || !opinionConclusion.trim()} onClick={() => void run(async () => { await contractsService.issueOpinion(opinion.id, organizationId, unitId, { conclusion: opinionConclusion.trim() }); setOpinionConclusion(""); })}>Issue</button></>}</li>)}{opinions.length === 0 && <li>No opinions recorded.</li>}</ul></>}
        </section>}
        {organizationId && (canReadLegal || canCreateLegal) && <section>
            <h2>Law firms</h2>
            {canCreateLegal && <form className={styles.fields} onSubmit={createLawFirm}>
                <label>Code<input required value={firmCode} onChange={event => setFirmCode(event.target.value)} /></label>
                <label>Name<input required value={firmName} onChange={event => setFirmName(event.target.value)} /></label>
                <label>Email<input type="email" required value={firmEmail} onChange={event => setFirmEmail(event.target.value)} /></label>
                <button type="submit" disabled={busy}>Add law firm</button>
            </form>}
            <ul>{lawFirms.map(firm => <li key={firm.id}>{firm.name} ({firm.code}) — {firm.active ? "Active" : "Inactive"}</li>)}</ul>
        </section>}
        {organizationId && (canReadAuthorities || canCreateAuthorities) && <section>
            <h2>Powers of attorney</h2>
            {canCreateAuthorities && <form className={styles.fields} onSubmit={createAuthority}>
                <label>Reference<input required value={authorityReference} onChange={event => setAuthorityReference(event.target.value)} /></label>
                <label>Principal ID<input required value={authorityPrincipal} onChange={event => setAuthorityPrincipal(event.target.value)} /></label>
                <label>Attorney name<input required value={authorityAttorney} onChange={event => setAuthorityAttorney(event.target.value)} /></label>
                <label>Authority scope<input required value={authorityScope} onChange={event => setAuthorityScope(event.target.value)} /></label>
                <label>Issued on<input type="date" required value={authorityIssuedOn} onChange={event => setAuthorityIssuedOn(event.target.value)} /></label>
                <label>Expires on<input type="date" value={authorityExpiresOn} onChange={event => setAuthorityExpiresOn(event.target.value)} /></label>
                <button type="submit" disabled={busy}>Register authority</button>
            </form>}
            <ul>{authorities.map(authority => <li key={authority.id}>{authority.reference}: {authority.attorneyName}, {authority.scope} — {authority.status}
                {authority.status === "ACTIVE" && canApproveAuthorities && <button type="button" disabled={busy} onClick={() => { const reason = window.prompt("Reason for revocation"); if (reason?.trim()) void run(() => contractsService.revokePowerOfAttorney(authority.id, organizationId, unitId, reason.trim())); }}>Revoke</button>}</li>)}</ul>
        </section>}
    </div>;
}

