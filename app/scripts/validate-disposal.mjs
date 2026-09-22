import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { chromium, request } from 'playwright';

// Requires a dedicated PostgreSQL API, with login and permissions enabled.
// Bootstrap only that database with disposal-validation-bootstrap.sql first.
const apiURL = process.env.DISPOSAL_API_URL || 'http://localhost:8115';
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
        form: { username: 'disposal-validation', password: 'Disposal-validation-015' },
        headers: { [csrf.headerName]: csrf.token },
    });
    assert.equal(login.status(), 204, await login.text());
    const session = await call('GET', '/api/auth/session');
    assert.equal(session.requireLogin, true);
    assert.equal(session.access.enforced, true);
    const suffix = Date.now();
    const organizationId = await create('core/organizations', { name: `Disposal ${suffix}`, nature: 'Validation', active: true, publicOrganization: false });
    const unitId = await create('core/units', { organizationId, code: `U-${suffix}`, name: `Source ${suffix}`, type: 'Unit' });
    const destinationUnitId = await create('core/units', { organizationId, code: `D-${suffix}`, name: `Destination ${suffix}`, type: 'Unit' });
    const locationId = await create('inventory/locations', { organizationId, unitId, name: `Store ${suffix}`, type: 'Warehouse', controlled: true });
    const destinationLocationId = await create('inventory/locations', { organizationId, unitId: destinationUnitId, name: `Destination store ${suffix}`, type: 'Warehouse', controlled: true });
    const personId = await create('core/people', { personType: 'INDIVIDUAL', fullName: `Operator ${suffix}`, active: true });
    const brandId = await create('inventory/brands', { name: `Brand ${suffix}`, manufacturer: 'Validation' });
    const categoryId = await create('inventory/categories', { name: `Assets ${suffix}`, family: 'GENERAL', serialized: true, lotControlled: false, consumable: false });
    const modelId = await create('inventory/models', { categoryId, brandId, name: `Asset ${suffix}`, unitOfMeasure: 'EA', sku: `SKU-${suffix}`, listPrice: '10' });
    const assets = [];
    for (const name of ['logical', 'physical', 'custody', 'maintenance', 'reservation', 'transfer']) {
        const assetCode = `${name}-${suffix}`;
        assets.push({ name, assetCode, id: await create('inventory/assets', { modelId, locationId, assetCode, serialNumber: assetCode, condition: 'GOOD', status: 'AVAILABLE', currentValue: '10' }) });
    }
    const lotCategoryId = await create('inventory/categories', { name: `Lots ${suffix}`, family: 'GENERAL', serialized: false, lotControlled: true, consumable: true });
    const lotModelId = await create('inventory/models', { categoryId: lotCategoryId, brandId, name: `Supply ${suffix}`, unitOfMeasure: 'EA', sku: `LOT-${suffix}`, listPrice: '1' });
    const lotId = await create('inventory/lots', { modelId: lotModelId, openingLocationId: locationId, lotNumber: `LOT-${suffix}`, initialQuantity: '10' });
    const stock = await call('GET', `disposals/stock?organizationId=${organizationId}&kind=LOT`);
    const balanceId = stock.content[0].stockId;
    const scope = { organizationId, unitId };
    const payload = items => ({ ...scope, requestId: randomUUID(), processNumber: randomUUID(), reason: 'Unserviceable', confirmed: true, items });
    const disposeAsset = asset => payload([{ assetId: asset.id, quantity: 1 }]);
    const recovery = await call('POST', 'lifecycle/occurrences', { ...scope, assetId: assets[0].id, type: 'RECOVERY', description: 'Pending before disposal' }, 201);
    await call('POST', 'custodies', { ...scope, requestId: randomUUID(), recipientId: personId, authorizerId: personId, purpose: 'Validation', assetIds: [assets[2].id] });
    await call('POST', 'disposals', disposeAsset(assets[2]), 409);
    await call('POST', 'maintenance/orders', { ...scope, requestId: randomUUID(), assetId: assets[3].id, reason: 'Repair' });
    await call('POST', 'disposals', disposeAsset(assets[3]), 409);
    const reservation = await call('POST', 'reservations', { ...scope, requestId: randomUUID(), purpose: 'Reserved validation', startsAt: '2026-09-20T10:00:00', endsAt: '2027-09-20T10:00:00', items: [{ assetId: assets[4].id, quantity: 1 }, { balanceId, quantity: 1 }] });
    await call('POST', 'disposals', disposeAsset(assets[4]), 409);
    await call('POST', 'disposals', payload([{ balanceId, quantity: 1 }]), 409);
    await call('POST', `reservations/${reservation.id}/cancel`);
    const transfer = await call('POST', 'transfers', { organizationId, sourceUnitId: unitId, destinationUnitId, destinationLocationId, requestId: randomUUID(), purpose: 'Validation transfer', items: [{ assetId: assets[5].id, quantity: 1 }] });
    assert.equal(transfer.status, 'FINALIZED');
    await call('POST', 'disposals', disposeAsset(assets[5]), 400);
    const inventory = await call('POST', 'inventory-counts', { ...scope, requestId: randomUUID(), locationId, purpose: 'Validation count' });
    await call('POST', 'disposals', disposeAsset(assets[0]), 409);
    await call('POST', 'disposals', payload([{ balanceId, quantity: 1 }]), 409);
    await call('PUT', `inventory-counts/${inventory.id}/count`, { items: inventory.items.map(item => ({ itemId: item.id, countedQuantity: item.systemQuantity })) });
    await call('POST', 'disposals', disposeAsset(assets[0]), 409);
    await call('POST', `inventory-counts/${inventory.id}/cancel`);
    console.log('PASS PostgreSQL: custody, maintenance, reservation, OPEN/COUNTED inventory, transferred source scope');

    const profileId = await create('core/profiles', { name: `Disposal operator ${suffix}`, level: 'UNIT' });
    const userId = await create('core/users', { personId, login: `restricted-${suffix}`, password: 'Disposal-validation-015', blocked: false });
    await create('core/user-profiles', { userId, profileId, organizationId, unitId });
    async function grant(action) {
        const permissions = await call('GET', 'core/permissions?size=100');
        const permissionId = permissions.content.find(p => p.resource === 'disposals' && p.action === action)?.id
            || await create('core/permissions', { resource: 'disposals', action });
        return create('core/profile-permissions', { profileId, permissionId });
    }
    await grant('READ'); await grant('CREATE');
    const restricted = await request.newContext({ baseURL: apiURL });
    try {
        const token = await (await restricted.get('/api/auth/csrf')).json();
        assert.equal((await restricted.post('/api/auth/login', { form: { username: `restricted-${suffix}`, password: 'Disposal-validation-015' }, headers: { [token.headerName]: token.token } })).status(), 204);
        const csrf = await (await restricted.get('/api/auth/csrf')).json();
        const headers = { [csrf.headerName]: csrf.token };
        const data = disposeAsset(assets[4]);
        const send = body => restricted.post('/api/erp/disposals', { data: body, headers });
        assert.equal((await send(data)).status(), 403, 'CREATE alone must not finalize');
        const approval = await grant('APPROVE');
        assert.equal((await send({ ...data, confirmed: false })).status(), 400);
        assert.equal((await send({ ...data, unitId: destinationUnitId })).status(), 403);
        const finalized = await send(data);
        assert.equal(finalized.status(), 200, await finalized.text());
        await call('DELETE', `core/profile-permissions/${approval}?version=0`, undefined, 204);
        assert.equal((await send(data)).status(), 403, 'Revocation applies to retries');
        console.log('PASS PostgreSQL authorization: CREATE denied, scoped APPROVE required, confirmation required, revocation enforced');
    } finally { await restricted.dispose(); }

    // Proxy only to the real isolated API, preserving its session and CSRF handling.
    let submitted = [], loseResponse = false;
    await context.route('**/api/**', async route => {
        const req = route.request(), url = new URL(req.url());
        const response = await api.fetch(`${apiURL}${url.pathname}${url.search}`, {
            method: req.method(), data: req.postDataBuffer() || undefined,
            headers: Object.fromEntries(Object.entries(req.headers()).filter(([key]) => !['host', 'cookie', 'content-length'].includes(key))),
        });
        if (url.pathname === '/api/erp/disposals' && req.method() === 'POST') {
            submitted.push(req.postDataJSON());
            if (loseResponse && response.status() === 200) { loseResponse = false; await route.abort('failed'); return; }
        }
        await route.fulfill({ response });
    });
    await context.addInitScript(() => localStorage.setItem('comandos-locale', 'en-US'));
    for (const [index, physical] of [false, true].entries()) {
        const asset = assets[index], processNumber = `BROWSER-${asset.assetCode}`;
        await page.goto(`${appURL}/erp/disposals`);
        await page.locator('#core-organizationId').selectOption(String(organizationId));
        await page.locator('#core-unitId').selectOption(String(unitId));
        await page.locator('#disposal-process').fill(processNumber);
        await page.locator('#disposal-reason').fill('Unserviceable validation stock');
        await page.getByRole('row').filter({ hasText: asset.assetCode }).getByRole('button', { name: 'Add', exact: true }).click();
        if (physical) {
            await page.getByRole('checkbox').check();
            await page.locator('#destruction-method').fill('Certified dismantling');
            await page.locator('#destroyed-at').fill('2026-09-20T10:00');
            await page.locator('#destruction-certificate').fill(`CERT-${suffix}`);
            await page.locator('#disposal-kind').selectOption('LOT');
            await page.getByRole('row').filter({ hasText: `LOT-${suffix}` }).getByRole('button', { name: 'Add', exact: true }).click();
            await page.getByRole('spinbutton', { name: `Quantity for LOT-${suffix}`, exact: true }).fill('2.5');
        }
        const submit = page.getByRole('button', { name: 'Finalize disposal', exact: true });
        const before = submitted.length;
        page.once('dialog', async dialog => { assert.match(dialog.message(), /permanently/); await dialog.dismiss(); });
        await submit.click();
        assert.equal(submitted.length, before);
        page.once('dialog', dialog => dialog.accept());
        loseResponse = physical;
        await submit.click();
        if (physical) {
            await page.getByText('The result could not be confirmed.', { exact: false }).waitFor();
            await page.getByRole('button', { name: 'Retry finalize', exact: true }).click();
        }
        await page.getByText(/Disposal process #\d+ finalized successfully/).waitFor();
        if (physical) assert.deepEqual(submitted.at(-1), submitted.at(-2));
        const details = page.locator('details').filter({ hasText: processNumber });
        await details.locator('summary').click();
        await details.getByText(physical ? `CERT-${suffix}` : 'Logical disposal only.', { exact: false }).waitFor();
        const current = await call('GET', `inventory/assets/${asset.id}`);
        assert.equal(current.status, 'DISPOSED');
        await call('PUT', `inventory/assets/${asset.id}`, { ...current, status: 'AVAILABLE' }, 400);
        await call('POST', 'disposals', disposeAsset(asset), 409);
        await call('POST', 'lifecycle/inspections', { ...scope, assetId: asset.id, checklist: 'Validation', result: 'FAILED' }, 409);
        await call('POST', 'lifecycle/occurrences', { ...scope, assetId: asset.id, type: 'DAMAGE', description: 'Must preserve disposal' }, 409);
        if (!physical) await call('POST', `lifecycle/occurrences/${recovery.id}/resolve`, {}, 409);
        const available = await call('GET', `disposals/stock?organizationId=${organizationId}&kind=ASSET`);
        assert.ok(!available.content.some(item => item.stockId === asset.id));
        const history = await call('GET', `disposals?organizationId=${organizationId}`);
        const record = history.content.find(item => item.processNumber === processNumber);
        assert.equal(record.finalizedByLogin, 'disposal-validation');
        assert.equal(record.destructionCertificate, physical ? `CERT-${suffix}` : null);
        const audit = await call('GET', `audit?resource=disposals&recordId=${record.id}`);
        assert.ok(audit.content.some(item => item.action === 'FINALIZE'));
        console.log(`PASS browser/PostgreSQL: ${physical ? 'physical destruction + lot + lost response retry' : 'logical disposal'}, cancellation, history, audit, terminal status`);
    }
    const lot = await call('GET', `inventory/lots/${lotId}`);
    assert.equal(Number(lot.availableQuantity), 7.5);
    assert.deepEqual(errors, []);
    console.log(`PASS integrated disposal validation; organization=${organizationId}`);
} finally {
    await context.close(); await browser.close(); await api.dispose();
}
