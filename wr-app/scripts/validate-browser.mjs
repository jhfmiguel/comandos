import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const { chromium, request } = createRequire(import.meta.url)('playwright');

// Run only against the separate validation API/database documented in docs/validation.md.
const apiURL = 'http://localhost:8180';
const appURL = 'http://localhost:3100';

async function main() {
    const api = await request.newContext({ baseURL: apiURL });
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(15000);
    const errors = [];
    let delayedSearch;
    page.on('pageerror', error => errors.push(error.message));
    // Use the existing production build while forwarding its API calls to the test instance.
    await context.route('http://localhost:8080/api/**', async route => {
        try {
            const response = await route.fetch({ url: route.request().url().replace('localhost:8080', 'localhost:8180') });
            if (delayedSearch && new URL(route.request().url()).searchParams.get('name') === delayedSearch.name) {
                delayedSearch.started();
                await delayedSearch.release;
            }
            if (response.status() >= 500) errors.push(`${response.status()} ${route.request().url()}`);
            await route.fulfill({ response });
        } catch (error) {
            if (!route.request().failure()) errors.push(error.message);
            await route.abort().catch(() => {});
        }
    });
    async function create(path, data) {
        const response = await api.post(`/api/erp/${path}`, { data });
        assert.equal(response.status(), 201, await response.text());
        return (await response.json()).id;
    }
    async function get(path) {
        const response = await api.get(`/api/erp/${path}`);
        assert.equal(response.status(), 200, await response.text());
        return response.json();
    }
    try {
        const suffix = Date.now().toString();
        const organizationId = await create('core/organizations', { name: `Browser validation ${suffix}`, nature: 'Public safety', publicOrganization: true, active: true });
        const unitId = await create('core/units', { organizationId, code: `UNIT-${suffix}`, name: 'Validation unit', type: 'Unit' });
        const recipientId = await create('core/people', { personType: 'INDIVIDUAL', fullName: `Recipient ${suffix}`, active: true });
        const authorizerId = await create('core/people', { personType: 'INDIVIDUAL', fullName: `Authorizer ${suffix}`, active: true });
        const categoryId = await create('inventory/categories', { name: `Optical ${suffix}`, family: 'OPTICAL', serialized: true, lotControlled: false, consumable: false });
        const brandId = await create('inventory/brands', { name: `Brand ${suffix}`, manufacturer: 'Validation' });
        const modelId = await create('inventory/models', { categoryId, brandId, name: 'Validation equipment', unitOfMeasure: 'EA', sku: `MODEL-${suffix}`, listPrice: '1000' });
        const locationId = await create('inventory/locations', { organizationId, unitId, name: 'Validation stock', type: 'Controlled', controlled: true });
        const assetCode = `ASSET-${suffix}`;
        const assetId = await create('inventory/assets', { modelId, locationId, assetCode, serialNumber: `SN-${suffix}`, condition: 'GOOD', status: 'AVAILABLE', currentValue: '1000' });
        const conditions = await get('inventory/custody-return-condition-types');
        const damaged = conditions.content.find(value => value.code === 'DAMAGED');
        assert.ok(damaged?.blocksAvailability);

        await page.goto(`${appURL}/erp/custody`);
        await page.locator('#core-organizationId').selectOption(String(organizationId));
        await page.locator('#core-unitId').selectOption(String(unitId));
        await page.locator('#core-recipientId').selectOption(String(recipientId));
        await page.locator('#core-authorizerId').selectOption(String(authorizerId));
        await page.getByLabel('Purpose *', { exact: true }).fill('Browser acceptance validation');
        await page.getByRole('row').filter({ hasText: assetCode }).getByRole('button', { name: 'Add', exact: true }).click();
        await page.getByRole('button', { name: 'Issue custody', exact: true }).click();
        await page.getByText(/Custody #\d+ issued successfully\./).waitFor();
        assert.equal((await get(`inventory/assets/${assetId}`)).status, 'CUSTODIED');
        await page.locator('#core-returnConditionTypeId').selectOption(String(damaged.id));
        await page.getByLabel('Inspection notes', { exact: true }).fill('Lens damaged during validation');
        await page.locator('summary').filter({ hasText: `Recipient ${suffix}` }).click();
        await page.getByRole('button', { name: 'Return', exact: true }).click();
        await page.getByRole('button', { name: 'Open maintenance', exact: true }).waitFor();
        assert.equal((await get(`inventory/assets/${assetId}`)).status, 'BLOCKED');
        await page.getByRole('button', { name: 'Open maintenance', exact: true }).click();
        await page.waitForURL('**/erp/maintenance?**');
        await page.waitForFunction(() => document.querySelector('#core-organization')?.value !== '');
        await page.getByRole('button', { name: 'Open work order', exact: true }).click();
        await page.getByText(/Work order #\d+ opened successfully\./).waitFor();
        assert.equal((await get(`inventory/assets/${assetId}`)).status, 'IN_MAINTENANCE');
        console.log('PASS PostgreSQL/browser: custody issue -> damaged return -> linked maintenance');

        const routes = [
            ['/erp/core', 'Institutional core'], ['/erp/inventory', 'Assets and inventory'],
            ['/erp/sales', 'Inventory sales'], ['/erp/ammunition-consumption', 'Ammunition consumption'],
            ['/erp/donations', 'Donations'], ['/erp/transfers', 'Inventory transfers'],
            ['/erp/disposals', 'Asset disposal'], ['/erp/reservations', 'Inventory reservations'],
            ['/erp/inventory-counts', 'Physical inventory'], ['/erp/audit', 'Audit history'],
            ['/queries/users', 'Users'], ['/queries/weapons', 'Weapons'],
        ];
        for (const [path, title] of routes) {
            await page.goto(`${appURL}${path}`);
            await page.getByText(title, { exact: true }).first().waitFor();
            await page.waitForLoadState('networkidle');
            assert.equal(await page.getByText('Unable to reach the API.', { exact: false }).count(), 0, path);
            console.log(`PASS page ${path}`);
        }

        for (const resource of ['users', 'weapons']) {
            const slowName = `Slow ${resource} ${suffix}`;
            const fastName = `Fast ${resource} ${suffix}`;
            for (const [index, name] of [slowName, fastName].entries()) {
                const data = resource === 'users'
                    ? { name, cpf: `${suffix.slice(-10)}${index}`, birth: '1990-01-01', address: 'Validation', email: `validation${index}@example.com`, phone: '11999999999' }
                    : { name, sku: `${resource}-${suffix}-${index}`, description: 'Validation', price: 10 };
                const response = await api.post(`/api/${resource}`, { data });
                assert.equal(response.status(), 200, await response.text());
            }
            await page.goto(`${appURL}/queries/${resource}`);
            const search = page.getByPlaceholder('Search name...');
            await search.waitFor();
            let started;
            let release;
            const startedPromise = new Promise(resolve => { started = resolve; });
            const releasePromise = new Promise(resolve => { release = resolve; });
            delayedSearch = { name: slowName, started, release: releasePromise };
            try {
                await search.fill(slowName);
                await Promise.race([startedPromise, new Promise((_, reject) => {
                    const timer = setTimeout(() => reject(new Error('Delayed search was not requested')), 15000);
                    timer.unref();
                })]);
                const cancelled = page.waitForEvent('requestfailed', { predicate: req => new URL(req.url()).searchParams.get('name') === slowName });
                await search.fill(fastName);
                await page.getByText(fastName, { exact: true }).waitFor();
                await cancelled;
                release();
                await page.waitForLoadState('networkidle');
                assert.equal(await page.getByText(slowName, { exact: true }).count(), 0);
                assert.equal(await page.getByText(fastName, { exact: true }).count(), 1);
                console.log(`PASS ${resource}: outdated search cancelled; latest results retained`);
            } finally {
                release();
                delayedSearch = undefined;
            }
        }
        assert.deepEqual(errors, [], 'Browser exceptions or server errors');
        console.log(JSON.stringify({ organizationId, assetId, assetCode, result: 'PASS' }));
    } catch (error) {
        await page.screenshot({ path: '../wr-api/target/browser-validation/failure.png', fullPage: true }).catch(() => {});
        throw error;
    } finally {
        await browser.close();
        await api.dispose();
    }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
