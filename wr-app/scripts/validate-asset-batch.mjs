import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const { chromium, request } = createRequire(import.meta.url)('playwright');

// Isolated validation API; never point this script at the primary database.
const apiUrl = process.env.ASSET_BATCH_API_URL ?? 'http://localhost:8180';
const appUrl = process.env.ASSET_BATCH_APP_URL ?? 'http://localhost:3100';
const api = await request.newContext({ baseURL: apiUrl });
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const context = await browser.newContext();
const page = await context.newPage();
page.setDefaultTimeout(20000);
const errors = [];
let retryPath = '';
let retryBodies = [];
page.on('pageerror', error => errors.push(error.message));
await context.route('http://localhost:8080/api/**', async route => {
    const url = route.request().url().replace('http://localhost:8080', apiUrl);
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
    const locationName = `Intake location ${suffix}`;
    const locationId = await create('inventory/locations', { organizationId, unitId, name: locationName, type: 'Warehouse', controlled: true });
    const brandId = await create('inventory/brands', { name: `Intake brand ${suffix}`, manufacturer: 'Validation' });
    const serializedCategory = await create('inventory/categories', { name: `Serialized ${suffix}`, family: 'OPTICAL', serialized: true, lotControlled: false, consumable: false });
    const modelName = `Individual model ${suffix}`;
    const modelId = await create('inventory/models', { name: modelName, categoryId: serializedCategory, brandId, unitOfMeasure: 'EA', sku: `IND-${suffix}`, listPrice: '10' });

    await page.goto(`${appUrl}/erp/inventory?resource=assets`);
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
    await page.getByRole('button', { name: 'Confirm registration', exact: true }).waitFor();
    assert.equal((await get(`inventory/assets?organizationId=${organizationId}`)).content.length, 0);
    await page.getByRole('button', { name: 'Edit batch', exact: true }).click();
    assert.equal(await page.getByLabel('Asset code 1', { exact: true }).inputValue(), `CODE-A-${suffix}`);
    await page.getByRole('button', { name: 'Register assets', exact: true }).click();
    await page.getByRole('button', { name: 'Confirm registration', exact: true }).click();
    await page.getByRole('button', { name: 'Retry entry', exact: true }).waitFor();
    assert.ok(await page.getByLabel('Asset code 1', { exact: true }).isDisabled());
    await recover();
    await page.getByText('2 individual assets registered successfully.', { exact: true }).waitFor();
    assert.equal(await page.getByRole('cell', { name: 'Accepted; saved', exact: true }).count(), 2);
    await page.getByRole('button', { name: 'Done', exact: true }).click();
    const assets = (await get(`inventory/assets?organizationId=${organizationId}`)).content;
    assert.equal(assets.length, 2);
    assert.deepEqual(assets.map(a => [a.assetCode, a.serialNumber]), [[`CODE-A-${suffix}`, `SN-A-${suffix}`], [`CODE-B-${suffix}`, `SN-B-${suffix}`]]);
    console.log('PASS asset pairs, serial count, duplicate validation and identical retries');
    await page.getByRole('button', { name: 'New record', exact: true }).click();
    await choose('Model', modelId, modelName); await choose('Location', locationId, locationName);
    await page.getByLabel('Asset code 1', { exact: true }).fill('CODE-A-' + suffix);
    await page.getByLabel('Serial number 1', { exact: true }).fill('SN-A-' + suffix);
    await page.getByRole('button', { name: 'Add row', exact: true }).click();
    await page.getByLabel('Asset code 2', { exact: true }).fill('CODE-C-' + suffix);
    await page.getByLabel('Serial number 2', { exact: true }).fill('SN-C-' + suffix);
    await page.getByRole('button', { name: 'Register assets', exact: true }).click();
    await page.getByRole('cell', { name: 'Asset code is already registered. Serial number is already registered.', exact: true }).waitFor();
    assert.equal(await page.getByLabel('Asset code 2', { exact: true }).inputValue(), 'CODE-C-' + suffix);
    assert.equal((await get('inventory/assets?organizationId=' + organizationId)).content.length, 2);
    await page.getByRole('button', { name: 'Remove row 1', exact: true }).click();
    await page.getByRole('button', { name: 'Register assets', exact: true }).click();
    await page.getByRole('button', { name: 'Confirm registration', exact: true }).click();
    await page.getByText('1 individual assets registered successfully.', { exact: true }).waitFor();
    assert.equal((await get('inventory/assets?organizationId=' + organizationId)).content.length, 3);
    assert.deepEqual(errors, []);
    console.log('PASS per-row rejections, preserved valid pair, removal and corrected confirmation');


} catch (error) {
    await page.screenshot({ path: '../wr-api/target/browser-validation/asset-batch-failure.png', fullPage: true });
    throw error;
} finally { await browser.close(); await api.dispose(); }
