import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const { chromium, request } = createRequire(import.meta.url)('playwright');

// Isolated validation API; never point this script at the primary database.
const api = await request.newContext({ baseURL: 'http://localhost:8180' });
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const context = await browser.newContext();
const page = await context.newPage();
page.setDefaultTimeout(20000);
const errors = [];
let retryPath = '';
let retryBodies = [];
page.on('pageerror', error => errors.push(error.message));
await context.route('http://localhost:8080/api/**', async route => {
    const url = route.request().url().replace('localhost:8080', 'localhost:8180');
    if (retryPath && new URL(url).pathname === retryPath && route.request().method() === 'POST') {
        retryBodies.push(route.request().postDataJSON());
        if (retryBodies.length === 1) {
            const committed = await route.fetch({ url });
            assert.equal(committed.status(), 200, await committed.text());
            await route.abort('failed'); return;
        }
        if (retryBodies.length === 2) {
            await route.fulfill({ status: 403, contentType: 'application/problem+json', body: JSON.stringify({ detail: 'Validation retry permission failure' }) }); return;
        }
    }
    await route.continue({ url });
});
async function create(path, data) {
    const response = await api.post(`/api/erp/${path}`, { data });
    assert.equal(response.status(), 201, await response.text());
    return (await response.json()).id;
}
async function get(path) {
    const response = await api.get(`/api/erp/${path}`);
    assert.equal(response.status(), 200, await response.text()); return response.json();
}
async function choose(label, id, name) {
    await page.getByRole('searchbox', { name: `Search ${label.toLowerCase()}`, exact: true }).fill(name);
    await page.getByLabel(`${label} *`, { exact: true }).selectOption(String(id));
}
async function recover() {
    await page.getByRole('button', { name: 'Retry entry', exact: true }).click();
    await page.getByText('Validation retry permission failure', { exact: true }).waitFor();
    const recovered = page.waitForResponse(response => new URL(response.url()).pathname === retryPath && response.status() === 200);
    await page.getByRole('button', { name: 'Retry entry', exact: true }).click();
    await recovered;
    assert.equal(retryBodies.length, 3);
    assert.deepEqual(retryBodies[0], retryBodies[1]); assert.deepEqual(retryBodies[0], retryBodies[2]);
    retryPath = '';
}
try {
    const suffix = Date.now().toString();
    const organizationName = `Intake organization ${suffix}`;
    const organizationId = await create('core/organizations', { name: organizationName, nature: 'Public safety', publicOrganization: true, active: true });
    const unitName = `Issuing unit ${suffix}`;
    const unitId = await create('core/units', { organizationId, code: `ISS-${suffix}`, name: unitName, type: 'Unit' });
    const receivingName = `Receiving unit ${suffix}`;
    const recipientUnitId = await create('core/units', { organizationId, code: `REC-${suffix}`, name: receivingName, type: 'Unit' });
    const personName = `Authorizer ${suffix}`;
    const authorizerId = await create('core/people', { personType: 'INDIVIDUAL', fullName: personName, active: true });
    const locationName = `Intake location ${suffix}`;
    const locationId = await create('inventory/locations', { organizationId, unitId, name: locationName, type: 'Warehouse', controlled: true });
    const brandId = await create('inventory/brands', { name: `Intake brand ${suffix}`, manufacturer: 'Validation' });
    const serializedCategory = await create('inventory/categories', { name: `Serialized ${suffix}`, family: 'OPTICAL', serialized: true, lotControlled: false, consumable: false });
    const ammoCategory = await create('inventory/categories', { name: `Ammunition ${suffix}`, family: 'AMMUNITION', serialized: false, lotControlled: true, consumable: true });
    const modelName = `Individual model ${suffix}`;
    const modelId = await create('inventory/models', { name: modelName, categoryId: serializedCategory, brandId, unitOfMeasure: 'EA', sku: `IND-${suffix}`, listPrice: '10' });
    const ammoName = `Ammunition model ${suffix}`;
    const ammoModelId = await create('inventory/models', { name: ammoName, categoryId: ammoCategory, brandId, unitOfMeasure: 'EA', sku: `AMM-${suffix}`, listPrice: '1' });

    await page.goto('http://localhost:3100/erp/inventory');
    await page.getByRole('button', { name: 'Individual assets', exact: true }).click();
    await page.getByRole('button', { name: 'New record', exact: true }).click();
    await choose('Model', modelId, modelName); await choose('Location', locationId, locationName);
    await page.getByText('Paste a list from a spreadsheet', { exact: true }).click();
    await page.locator('#asset-pairs-paste').fill(`CODE-A-${suffix}\tSN-A-${suffix}\nCODE-B-${suffix}\tSN-B-${suffix}`);
    await page.getByRole('button', { name: 'Add pasted rows', exact: true }).click();
    await page.getByText('Quantity (serial numbers): 2', { exact: true }).waitFor();
    await page.getByLabel('Serial number 2', { exact: true }).fill(`SN-A-${suffix}`);
    assert.ok(await page.getByRole('button', { name: 'Register assets', exact: true }).isDisabled());
    await page.getByLabel('Serial number 2', { exact: true }).fill(`SN-B-${suffix}`);
    retryPath = '/api/erp/inventory/assets/batch'; retryBodies = [];
    await page.getByRole('button', { name: 'Register assets', exact: true }).click();
    await page.getByRole('button', { name: 'Retry entry', exact: true }).waitFor();
    assert.ok(await page.getByLabel('Asset code 1', { exact: true }).isDisabled());
    await recover();
    await page.getByText('2 individual assets registered successfully.', { exact: true }).waitFor();
    const assets = (await get(`inventory/assets?organizationId=${organizationId}`)).content;
    assert.equal(assets.length, 2);
    assert.deepEqual(assets.map(a => [a.assetCode, a.serialNumber]), [[`CODE-A-${suffix}`, `SN-A-${suffix}`], [`CODE-B-${suffix}`, `SN-B-${suffix}`]]);
    console.log('PASS asset pairs, serial count, duplicate validation and identical retries');

    await page.getByRole('button', { name: 'Stock lots', exact: true }).click();
    await page.getByRole('button', { name: 'Receive ammunition boxes', exact: true }).click();
    await choose('Model', ammoModelId, ammoName); await choose('Opening location', locationId, locationName);
    await page.getByLabel('Lot number *', { exact: true }).fill(`BOX-LOT-${suffix}`);
    await page.getByLabel('Number of boxes 1', { exact: true }).fill('10');
    await page.getByLabel('Rounds per box 1', { exact: true }).fill('50');
    await page.getByRole('button', { name: 'Add row', exact: true }).click();
    await page.getByLabel('Number of boxes 2', { exact: true }).fill('2');
    await page.getByLabel('Rounds per box 2', { exact: true }).fill('25');
    await page.getByText('Total rounds: 550', { exact: true }).waitFor();
    await page.getByLabel('Number of boxes 2', { exact: true }).fill('0.5');
    assert.ok(await page.getByRole('button', { name: 'Receive boxes', exact: true }).isDisabled());
    await page.getByLabel('Number of boxes 2', { exact: true }).fill('2');
    retryPath = '/api/erp/inventory/lots/from-boxes'; retryBodies = [];
    await page.getByRole('button', { name: 'Receive boxes', exact: true }).click();
    await page.getByRole('button', { name: 'Retry entry', exact: true }).waitFor(); await recover();
    await page.getByText('550 rounds received successfully.', { exact: true }).waitFor();
    const lots = (await get(`inventory/lots?organizationId=${organizationId}`)).content;
    assert.equal(lots.length, 1); assert.equal(Number(lots[0].initialQuantity), 550);
    assert.equal(lots[0].openingPackaging, '10 x 50 + 2 x 25');
    const balances = (await get(`inventory/balances?organizationId=${organizationId}`)).content;
    assert.equal(Number(balances[0].available), 550);
    console.log('PASS mixed ammunition box sizes, whole quantities and retry without double stock');

    await page.goto('http://localhost:3100/erp/custody');
    await choose('Organization', organizationId, organizationName);
    await page.getByRole('searchbox', { name: 'Search unit', exact: true }).fill(unitName);
    await page.getByLabel('Issuing unit', { exact: true }).selectOption(String(unitId));
    await page.getByLabel('Recipient type *', { exact: true }).selectOption('UNIT');
    await choose('Receiving organizational unit', recipientUnitId, receivingName);
    await choose('Authorizer', authorizerId, personName);
    await page.getByLabel('Purpose *', { exact: true }).fill('Unit recipient acceptance');
    await page.getByRole('row').filter({ hasText: assets[0].assetCode }).getByRole('button', { name: 'Add', exact: true }).click();
    await page.getByRole('button', { name: 'Issue custody', exact: true }).click();
    await page.getByText(/Custody #\d+ issued successfully\./).waitFor();
    const custody = (await get(`custodies?organizationId=${organizationId}`)).content[0];
    assert.equal(custody.recipientType, 'UNIT'); assert.equal(custody.recipientId, null); assert.equal(custody.recipientUnitId, recipientUnitId);
    assert.equal(custody.unitId, unitId);
    await page.locator('summary').filter({ hasText: receivingName }).click();
    await page.getByText(`Receiving organizational unit: ${receivingName}`, { exact: true }).waitFor();
    const good = (await get('inventory/custody-return-condition-types')).content.find(c => c.code === 'GOOD');
    await page.locator('#core-returnConditionTypeId').selectOption(String(good.id));
    await page.getByRole('button', { name: 'Return', exact: true }).click();
    await page.locator('summary').filter({ hasText: 'RETURNED' }).waitFor();
    assert.equal((await get(`inventory/assets/${assets[0].id}`)).status, 'AVAILABLE');
    assert.deepEqual(errors, []);
    console.log(`PASS unit custody and return; organization ${organizationId}`);
} catch (error) {
    await page.screenshot({ path: '../wr-api/target/browser-validation/stock-intake-failure.png', fullPage: true });
    throw error;
} finally { await browser.close(); await api.dispose(); }
