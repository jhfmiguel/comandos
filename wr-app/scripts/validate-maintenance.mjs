import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { chromium, request } from 'playwright';

// Requires a dedicated PostgreSQL API, with login and permissions enabled.
// Bootstrap only that database with maintenance-validation-bootstrap.sql first.
const apiURL = process.env.MAINTENANCE_API_URL || 'http://localhost:8114';
const appURL = process.env.APP_URL || 'http://localhost:3000';
assert.equal(new URL(apiURL).hostname, 'localhost');
assert.notEqual(new URL(apiURL).port, '8080', 'Never use the normal API for validation');
const api = await request.newContext({ baseURL: apiURL });
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const context = await browser.newContext();
const page = await context.newPage();
page.setDefaultTimeout(30000);
const errors = [];
page.on('pageerror', error => errors.push(error.message));
async function call(method, path, data, expected = 200) {
    const csrf = await (await api.get('/api/auth/csrf')).json();
    const response = await api.fetch(path.startsWith('/api/') ? path : `/api/erp/${path}`, {
        method, data, headers: { [csrf.headerName]: csrf.token },
    });
    assert.equal(response.status(), expected, `${method} ${path}: ${await response.text()}`);
    return response.status() === 204 ? null : response.json();
}
const create = async (path, data) => (await call('POST', path, data, 201)).id;
try {
    const csrf = await (await api.get('/api/auth/csrf')).json();
    const login = await api.post('/api/auth/login', {
        form: { username: 'maintenance-validation', password: 'Maintenance-validation-014' },
        headers: { [csrf.headerName]: csrf.token },
    });
    assert.equal(login.status(), 204, await login.text());
    const session = await call('GET', '/api/auth/session');
    assert.equal(session.requireLogin, true);
    assert.equal(session.access.enforced, true);
    const suffix = Date.now();
    const organizationId = await create('core/organizations', { name: `Maintenance ${suffix}`, nature: 'Validation', active: true, publicOrganization: false });
    const unitId = await create('core/units', { organizationId, code: `U-${suffix}`, name: `Source ${suffix}`, type: 'Unit' });
    const destinationUnitId = await create('core/units', { organizationId, code: `D-${suffix}`, name: `Destination ${suffix}`, type: 'Unit' });
    const locationId = await create('inventory/locations', { organizationId, unitId, name: `Store ${suffix}`, type: 'Warehouse', controlled: true });
    const destinationLocationId = await create('inventory/locations', { organizationId, unitId: destinationUnitId, name: `Destination store ${suffix}`, type: 'Warehouse', controlled: true });
    const personId = await create('core/people', { personType: 'INDIVIDUAL', fullName: `Operator ${suffix}`, active: true });
    const brandId = await create('inventory/brands', { name: `Brand ${suffix}`, manufacturer: 'Validation' });
    const categoryId = await create('inventory/categories', { name: `Assets ${suffix}`, family: 'GENERAL', serialized: true, lotControlled: false, consumable: false });
    const modelId = await create('inventory/models', { categoryId, brandId, name: `Asset ${suffix}`, unitOfMeasure: 'EA', sku: `SKU-${suffix}`, listPrice: '10' });
    const assets = [];
    for (const name of ['approved', 'rejected', 'inspection', 'expired']) {
        const assetCode = `${name}-${suffix}`;
        assets.push({ name, assetCode, id: await create('inventory/assets', { modelId, locationId, assetCode, serialNumber: assetCode, condition: 'GOOD', status: 'AVAILABLE', currentValue: '10' }) });
    }
    const scope = { organizationId, unitId };
    const completion = () => ({ requestId: randomUUID(), defect: 'Wear', cause: 'Use', opinion: 'Reviewed', services: [{ description: 'Inspection', cost: 0 }], testResult: 'APPROVED', testNotes: 'Passed' });
    let submitted = [], loseResponse = false;
    await context.route('**/api/**', async route => {
        const req = route.request(), url = new URL(req.url());
        const response = await api.fetch(`${apiURL}${url.pathname}${url.search}`, {
            method: req.method(), data: req.postDataBuffer() || undefined,
            headers: Object.fromEntries(Object.entries(req.headers()).filter(([key]) => !['host', 'cookie', 'content-length'].includes(key))),
        });
        if (url.pathname.endsWith('/complete') && req.method() === 'POST') {
            submitted.push(req.postDataJSON());
            if (loseResponse && response.status() === 200) { loseResponse = false; await route.abort('failed'); return; }
        }
        await route.fulfill({ response });
    });
    await context.addInitScript(() => localStorage.setItem('comandos-locale', 'en-US'));
    await page.goto(`${appURL}/erp/maintenance?organizationId=${organizationId}&unitId=${unitId}`);
    await page.getByText('Create maintenance plan', { exact: true }).click();
    await page.locator('#plan-name').fill(`Monthly ${suffix}`);
    await page.locator('#plan-days').fill('30');
    await page.getByRole('button', { name: 'Create plan', exact: true }).click();
    await page.locator('#maintenance-plan option').filter({ hasText: `Monthly ${suffix}` }).waitFor({ state: 'attached' });
    const planId = (await call('GET', `maintenance/plans?organizationId=${organizationId}&unitId=${unitId}`)).content[0].id;
    async function evidence(detail, resource, id, name) {
        const bytes = Buffer.from(`Evidence ${name}`);
        await detail.locator('input[type=file]').setInputFiles({ name, mimeType: 'text/plain', buffer: bytes });
        await detail.getByRole('button', { name: 'Attach evidence' }).click();
        await detail.getByRole('link', { name }).waitFor();
        const attachments = await call('GET', `lifecycle/attachments?resource=${resource}&recordId=${id}`);
        const download = await api.get(`/api/erp/lifecycle/attachments/${attachments[0].id}/content`);
        assert.equal(download.status(), 200); assert.deepEqual(await download.body(), bytes);
    }
    for (const [index, result] of ['APPROVED', 'REJECTED'].entries()) {
        const asset = assets[index];
        await page.locator('#maintenance-asset').selectOption(String(asset.id));
        await page.locator('#maintenance-plan').selectOption(String(planId));
        await page.locator('#maintenance-reason').fill(`Validation ${result}`);
        const openedResponse = page.waitForResponse(r => r.url().endsWith('/maintenance/orders') && r.request().method() === 'POST');
        await page.getByRole('button', { name: 'Open work order', exact: true }).click();
        assert.equal((await openedResponse).status(), 200);
        
        const order = (await call('GET', `maintenance/orders?organizationId=${organizationId}&unitId=${unitId}`)).content.find(o => o.assetId === asset.id);
        assert.equal((await call('GET', `inventory/assets/${asset.id}`)).status, 'IN_MAINTENANCE');
        await call('POST', 'custodies', { ...scope, requestId: randomUUID(), recipientId: personId, authorizerId: personId, purpose: 'Must be blocked', assetIds: [asset.id] }, 409);
        await call('POST', 'transfers', { organizationId, sourceUnitId: unitId, destinationUnitId, destinationLocationId, requestId: randomUUID(), purpose: 'Must be blocked', items: [{ assetId: asset.id, quantity: 1 }] }, 409);
        await call('POST', 'maintenance/orders', { ...scope, requestId: randomUUID(), assetId: asset.id, reason: 'Duplicate' }, 409);
        await call('POST', 'lifecycle/inspections', { ...scope, assetId: asset.id, checklist: 'Must not interrupt', result: 'FAILED', generateMaintenance: true }, 409);
        const current = await call('GET', `inventory/assets/${asset.id}`);
        await call('PUT', `inventory/assets/${asset.id}`, { version: current.version, modelId, locationId, assetCode: current.assetCode, serialNumber: current.serialNumber, condition: 'GOOD', status: 'AVAILABLE', currentValue: '10' }, 400);
        const detail = page.locator('details').filter({ has: page.locator('summary').filter({ hasText: asset.assetCode }) });
        await detail.locator('summary').click();
        for (const [field, value] of Object.entries({ defect: 'Wear', cause: 'Use', opinion: 'Inspection completed', service: 'Inspection and replacement', notes: `Test ${result}` })) await page.locator(`#${field}-${order.id}`).fill(value);
        await page.locator(`#cost-${order.id}`).fill('25.5');
        await page.locator(`#result-${order.id}`).selectOption(result);
        await detail.getByRole('button', { name: 'Add part', exact: true }).click();
        for (const [field, value] of Object.entries({ description: 'Replacement component', quantity: '2', unitCost: '3.25', partNumber: 'P-014' })) await page.locator(`#part-${field}-${order.id}-0`).fill(value);
        const button = detail.getByRole('button', { name: 'Complete work order', exact: true });
        const before = submitted.length;
        page.once('dialog', dialog => dialog.dismiss()); await button.click(); assert.equal(submitted.length, before);
        loseResponse = index === 1;
        page.once('dialog', dialog => dialog.accept()); await button.click();
        if (index === 1) { await detail.getByRole('button', { name: 'Retry completion', exact: true }).click(); assert.deepEqual(submitted.at(-1), submitted.at(-2)); }
        await page.getByText(`Work order #${order.id} completed.`, { exact: true }).waitFor();
        await detail.getByText('Replacement component', { exact: false }).waitFor();
        await detail.getByText('Total cost: 32.0000', { exact: false }).waitFor();
        const stored = (await call('GET', `maintenance/orders?organizationId=${organizationId}`)).content.find(o => o.id === order.id);
        assert.equal(stored.parts[0].partNumber, 'P-014'); assert.equal(stored.totalCost, '32.0000'); assert.ok(stored.nextMaintenanceAt); assert.equal(stored.completedByLogin, 'maintenance-validation');
        assert.equal((await call('GET', `inventory/assets/${asset.id}`)).status, index === 0 ? 'AVAILABLE' : 'BLOCKED');
        await call('POST', `maintenance/orders/${order.id}/complete`, { ...submitted.at(-1), parts: [] }, 409);
        const audit = await call('GET', `audit?resource=maintenance&recordId=${order.id}`);
        for (const action of ['OPEN', 'COMPLETE']) assert.ok(audit.content.some(a => a.action === action));
        await evidence(detail, 'maintenance', order.id, `evidence-${result}.txt`);
        console.log(`PASS browser/PostgreSQL ${result}: opening, services, parts, completion, availability, history, audit, evidence and retry`);
    }
    await page.goto(`${appURL}/erp/lifecycle`);
    const inputs = page.locator('input[inputmode=numeric]');
    await inputs.nth(0).fill(String(organizationId)); await inputs.nth(1).fill(String(unitId)); await inputs.nth(2).fill(String(assets[2].id));
    const inspectionForm = page.locator('form').filter({ has: page.getByRole('heading', { name: 'Periodic inspection', exact: true }) });
    await inspectionForm.locator('textarea').nth(0).fill('Periodic checklist');
    await inspectionForm.locator('select').selectOption('MAINTENANCE_REQUIRED');
    await inspectionForm.locator('textarea').nth(1).fill('Review needed');
    await inspectionForm.getByRole('button', { name: 'Record inspection', exact: true }).click();
    await page.getByText('Inspection recorded', { exact: true }).waitFor();
    const inspection = (await call('GET', `lifecycle/inspections?organizationId=${organizationId}`)).content[0]; assert.ok(inspection.generatedWorkOrderId);
    assert.equal((await call('GET', `inventory/assets/${assets[2].id}`)).status, 'IN_MAINTENANCE');
    const detail = page.locator('details').filter({ hasText: assets[2].assetCode });
    await detail.locator('summary').click(); await detail.getByRole('button', { name: 'Approve inspection' }).click();
    await detail.getByText('Approved by: maintenance-validation', { exact: false }).waitFor();
    await evidence(detail, 'periodic-inspections', inspection.id, 'inspection.txt');
    await call('POST', `maintenance/orders/${inspection.generatedWorkOrderId}/complete`, completion());
    assert.equal((await call('GET', `inventory/assets/${assets[2].id}`)).status, 'AVAILABLE');
    const expired = await call('GET', `inventory/assets/${assets[3].id}`);
    await call('PUT', `inventory/assets/${assets[3].id}`, { version: expired.version, modelId, locationId, assetCode: expired.assetCode, serialNumber: expired.serialNumber, condition: 'GOOD', status: 'AVAILABLE', currentValue: '10', validUntil: '2020-01-01' });
    const expiredOrder = await call('POST', 'maintenance/orders', { ...scope, requestId: randomUUID(), assetId: assets[3].id, reason: 'Expired validation' });
    await call('POST', `maintenance/orders/${expiredOrder.id}/complete`, completion());
    assert.equal((await call('GET', `inventory/assets/${assets[3].id}`)).status, 'BLOCKED');
    await call('POST', 'lifecycle/attachments', { resource: 'maintenance', recordId: 9223372036854, fileName: 'orphan.txt', contentType: 'text/plain', base64: 'YWJj' }, 404);
    assert.deepEqual(errors, []);
    console.log(`PASS periodic inspection, generated order, approval, evidence, return to service and expired asset blocking; organization=${organizationId}`);
} finally {
    await context.close(); await browser.close(); await api.dispose();
}
