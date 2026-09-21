import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { createRequire } from 'node:module';
const { chromium, request } = createRequire(import.meta.url)('playwright');

// Use only the separate wr_validation_20260912 database and test instances.
const appURL = 'http://localhost:3000';
const apiURL = 'http://localhost:8180';
const browserAPI = 'http://localhost:8080';

async function main() {
    const api = await request.newContext({ baseURL: apiURL });
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(15000);
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await context.route(`${browserAPI}/api/**`, route => route.continue({ url: route.request().url().replace(browserAPI, apiURL) }));
    async function get(path) {
        const response = await api.get(`/api/erp/${path}`);
        assert.equal(response.status(), 200, await response.text());
        return response.json();
    }
    async function create(path, data) {
        const response = await api.post(`/api/erp/${path}`, { data });
        assert.equal(response.status(), 201, `${path}: ${await response.text()}`);
        return (await response.json()).id;
    }
    async function loss(path, button, retryName) {
        const url = `${browserAPI}/api/erp/${path}`;
        const attempts = [];
        await context.route(url, async route => {
            if (route.request().method() !== 'POST') return route.continue({ url: route.request().url().replace(browserAPI, apiURL) });
            attempts.push(route.request().postDataJSON());
            if (attempts.length === 1) {
                const response = await route.fetch({ url: route.request().url().replace(browserAPI, apiURL) });
                assert.equal(response.status(), 200, await response.text());
                await route.abort('failed');
            } else if (attempts.length === 2) {
                await route.fulfill({ status: 403, contentType: 'application/problem+json',
                    body: JSON.stringify({ status: 403, detail: 'Recovery temporarily denied during validation.' }) });
            } else await route.continue({ url: route.request().url().replace(browserAPI, apiURL) });
        });
        await page.getByRole('button', { name: button, exact: true }).click();
        await page.getByText('Unable to complete the request.', { exact: false }).waitFor();
        await page.getByRole('button', { name: retryName, exact: true }).click();
        await page.getByText('Recovery temporarily denied during validation.', { exact: true }).waitFor();
        assert.equal(await page.locator('form select').first().isDisabled(), true);
        await Promise.all([
            page.waitForResponse(response => new URL(response.url()).pathname === `/api/erp/${path}` && response.request().method() === 'POST' && response.status() === 200),
            page.getByRole('button', { name: retryName, exact: true }).click(),
        ]);
        assert.equal(attempts.length, 3);
        assert.deepEqual(attempts[1], attempts[0]);
        assert.deepEqual(attempts[2], attempts[0]);
        await context.unroute(url);
    }
    async function action(button, path, status = 200) {
        await Promise.all([
            page.waitForResponse(response => new URL(response.url()).pathname === `/api/erp/${path}` && response.request().method() !== 'GET' && response.status() === status),
            page.getByRole('button', { name: button, exact: true }).click(),
        ]);
    }
    try {
        const suffix = Date.now().toString();
        const organizationName = `Stock workflow ${suffix}`;
        const org = await create('core/organizations', { name: organizationName, nature: 'Company', active: true, publicOrganization: false });
        const source = await create('core/units', { organizationId: org, code: `SRC-${suffix}`, name: 'Source', type: 'Unit' });
        const destination = await create('core/units', { organizationId: org, code: `DST-${suffix}`, name: 'Destination', type: 'Unit' });
        const sourceLocationName = `Source stock ${suffix}`;
        const destinationLocationName = `Destination stock ${suffix}`;
        const sourceLocation = await create('inventory/locations', { organizationId: org, unitId: source, name: sourceLocationName, type: 'Warehouse', controlled: true });
        const destinationLocation = await create('inventory/locations', { organizationId: org, unitId: destination, name: destinationLocationName, type: 'Warehouse', controlled: true });
        const brand = await create('inventory/brands', { name: `Brand ${suffix}`, manufacturer: 'Validation' });
        async function model(lot) {
            const category = await create('inventory/categories', { name: `Category ${lot} ${suffix}`, family: lot ? 'AMMUNITION' : 'OPTICAL', serialized: !lot, lotControlled: lot, consumable: lot });
            return create('inventory/models', { categoryId: category, brandId: brand, name: `Model ${lot} ${suffix}`, sku: `MODEL-${lot}-${suffix}`, unitOfMeasure: 'EA', listPrice: '10' });
        }
        const assetModel = await model(false);
        const lotModel = await model(true);
        const missingCode = `MISSING-${suffix}`;
        const transferableCode = `MOVABLE-${suffix}`;
        async function asset(assetCode) {
            return create('inventory/assets', { modelId: assetModel, locationId: sourceLocation, assetCode, serialNumber: randomUUID(), condition: 'GOOD', status: 'AVAILABLE', currentValue: '10' });
        }
        const missingAsset = await asset(missingCode);
        const transferableAsset = await asset(transferableCode);
        const lotCode = `LOT-${suffix}`;
        const lotId = await create('inventory/lots', { modelId: lotModel, openingLocationId: sourceLocation, lotNumber: lotCode, initialQuantity: '10' });
        const sourceBalance = (await get(`inventory/balances?organizationId=${org}`)).content.find(item => item.lotId === lotId);
        assert.ok(sourceBalance);
        async function scope(path, organizationField, unitField) {
            await page.goto(`${appURL}/erp/${path}`);
            await page.getByRole('searchbox', { name: 'Search organization', exact: true }).fill(organizationName);
            await page.locator(`#core-${organizationField}`).selectOption(String(org));
            await page.locator(`#core-${unitField}`).selectOption(String(source));
        }
        const purpose = `Reserve stock ${suffix}`;
        await scope('reservations', 'organization', 'unit');
        await page.locator('#reservation-purpose').fill(purpose);
        await page.locator('#reservation-start').fill(new Date(Date.now() + 86400000).toISOString().slice(0, 16));
        await page.locator('#reservation-end').fill(new Date(Date.now() + 172800000).toISOString().slice(0, 16));
        await page.getByRole('row').filter({ hasText: missingCode }).getByRole('button', { name: 'Add', exact: true }).click();
        await page.locator('select').filter({ has: page.locator('option[value="LOT"]') }).selectOption('LOT');
        await page.getByRole('row').filter({ hasText: lotCode }).getByRole('button', { name: 'Add', exact: true }).click();
        await page.getByRole('spinbutton', { name: `Quantity for ${lotCode}`, exact: true }).fill('3');
        await loss('reservations', 'Create reservation', 'Retry reservation');
        await page.getByText(/Reservation #\d+ created successfully\./).waitFor();
        const reservations = await get(`reservations?organizationId=${org}&unitId=${source}`);
        assert.equal(reservations.totalElements, 1);
        const reservationId = reservations.content[0].id;
        assert.equal((await get(`inventory/assets/${missingAsset}`)).status, 'BLOCKED');
        let balance = await get(`inventory/balances/${sourceBalance.id}`);
        assert.equal(balance.available, '7.0000'); assert.equal(balance.reserved, '3.0000');
        const saleStock = await get(`sales/stock?organizationId=${org}&unitId=${source}&kind=ASSET`);
        assert.equal(saleStock.content.some(item => item.stockId === missingAsset), false);
        const deniedTransfer = await api.post('/api/erp/transfers', { data: { requestId: randomUUID(), organizationId: org,
            sourceUnitId: source, destinationUnitId: destination, destinationLocationId: destinationLocation, purpose: 'Cannot use reserved stock',
            items: [{ balanceId: sourceBalance.id, quantity: '8' }] } });
        assert.equal(deniedTransfer.status(), 409, await deniedTransfer.text());
        console.log('PASS reservation: exact recovery, blocked equipment and reserved quantity unavailable for other operations');

        await scope('inventory-counts', 'organizationId', 'unitId');
        await page.getByRole('searchbox', { name: 'Search stock location', exact: true }).fill(sourceLocationName);
        await page.locator('#core-locationId').selectOption(String(sourceLocation));
        await page.locator('form input:not([type])').fill(`Count ${suffix}`);
        await loss('inventory-counts', 'Open inventory count', 'Retry opening count');
        await page.getByRole('heading', { name: /Inventory count #\d+/ }).waitFor();
        const counts = await get(`inventory-counts?organizationId=${org}&unitId=${source}`);
        assert.equal(counts.totalElements, 1);
        const countId = counts.content[0].id;
        const quantity = code => page.getByRole('spinbutton', { name: `Counted quantity for ${code}`, exact: true });
        await quantity(missingCode).fill('');
        assert.equal(await page.getByRole('button', { name: 'Save count', exact: true }).isDisabled(), true);
        await quantity(missingCode).fill('0.5');
        assert.equal(await page.getByRole('button', { name: 'Save count', exact: true }).isDisabled(), true);
        await quantity(missingCode).fill('0');
        await quantity(lotCode).fill('2');
        await action('Save count', `inventory-counts/${countId}/count`);
        await action('Approve adjustments', `inventory-counts/${countId}/approve`, 409);
        await page.getByText('Release reserved or blocked quantity before approving this shortage.', { exact: true }).waitFor();
        balance = await get(`inventory/balances/${sourceBalance.id}`);
        assert.equal(balance.available, '7.0000'); assert.equal(balance.reserved, '3.0000');
        let movements = await get(`inventory/movements?organizationId=${org}&size=100`);
        assert.equal(movements.content.filter(item => item.nature === 'INVENTORY_ADJUSTMENT').length, 0);
        console.log('PASS inventory: opening retry, blank/fractional individual count blocked, reserved shortage approval rolls back');

        await scope('reservations', 'organization', 'unit');
        await page.locator('summary').filter({ hasText: purpose }).click();
        await action('Cancel', `reservations/${reservationId}/cancel`);
        balance = await get(`inventory/balances/${sourceBalance.id}`);
        assert.equal(balance.available, '10.0000'); assert.equal(balance.reserved, '0.0000');
        assert.equal((await get(`inventory/assets/${missingAsset}`)).status, 'AVAILABLE');
        await scope('inventory-counts', 'organizationId', 'unitId');
        await page.locator('summary').filter({ hasText: `#${countId} ` }).click();
        await page.getByRole('button', { name: 'View', exact: true }).click();
        await quantity(lotCode).fill('8.25');
        await action('Save count', `inventory-counts/${countId}/count`);
        await action('Approve adjustments', `inventory-counts/${countId}/approve`);
        await page.getByRole('heading', { name: `Inventory count #${countId} · Approved`, exact: true }).waitFor();
        balance = await get(`inventory/balances/${sourceBalance.id}`);
        assert.equal(balance.available, '8.2500');
        assert.equal((await get(`inventory/assets/${missingAsset}`)).status, 'BLOCKED');
        assert.equal((await get(`inventory/assets/${transferableAsset}`)).status, 'AVAILABLE');
        movements = await get(`inventory/movements?organizationId=${org}&size=100`);
        assert.equal(movements.content.filter(item => item.nature === 'INVENTORY_ADJUSTMENT').length, 2);
        console.log('PASS cancellation releases reservation; inventory approval adjusts lot and blocks the missing asset');

        await scope('transfers', 'organizationId', 'sourceUnitId');
        await page.locator('#core-destinationUnitId').selectOption(String(destination));
        await page.getByRole('searchbox', { name: 'Search destination location', exact: true }).fill(destinationLocationName);
        await page.locator('#core-destinationLocationId').selectOption(String(destinationLocation));
        await page.locator('#transfer-purpose').fill(`Transfer ${suffix}`);
        await page.getByRole('row').filter({ hasText: transferableCode }).getByRole('button', { name: 'Add', exact: true }).click();
        await page.locator('#transfer-kind').selectOption('LOT');
        await page.getByRole('row').filter({ hasText: lotCode }).getByRole('button', { name: 'Add', exact: true }).click();
        await page.getByRole('spinbutton', { name: `Quantity for ${lotCode}`, exact: true }).fill('3.125');
        await loss('transfers', 'Finalize transfer', 'Retry finalize');
        await page.getByText(/Transfer #\d+ finalized successfully\./).waitFor();
        const transfers = await get(`transfers?organizationId=${org}&unitId=${source}`);
        assert.equal(transfers.totalElements, 1);
        const balances = (await get(`inventory/balances?organizationId=${org}&size=100`)).content.filter(item => item.lotId === lotId);
        assert.equal(balances.find(item => item.locationId === sourceLocation).available, '5.1250');
        assert.equal(balances.find(item => item.locationId === destinationLocation).available, '3.1250');
        assert.equal((await get(`inventory/lots/${lotId}`)).availableQuantity, '8.2500');
        assert.equal((await get(`inventory/assets/${transferableAsset}`)).locationId, destinationLocation);
        assert.equal((await get(`inventory/assets/${missingAsset}`)).locationId, sourceLocation);
        console.log('PASS mixed transfer: response-loss recovery, one transfer, preserved lot total and correct destination');
        assert.deepEqual(errors, []);
        console.log(JSON.stringify({ organizationId: org, reservationId, countId, transferId: transfers.content[0].id, result: 'PASS' }));
    } catch (error) {
        await page.screenshot({ path: '../wr-api/target/browser-validation/stock-workflows-failure.png', fullPage: true }).catch(() => {});
        throw error;
    } finally {
        await browser.close(); await api.dispose();
    }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
