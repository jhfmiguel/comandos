import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { createRequire } from 'node:module';
const { chromium, request } = createRequire(import.meta.url)('playwright');

// Both API instances must use only wr_validation_20260912 (see docs/validation.md).
const setupURL = 'http://localhost:8180';
const appURL = 'http://localhost:3000';

async function main() {
    const setup = await request.newContext({ baseURL: setupURL });
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(15000);
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await context.route('http://localhost:8080/api/**', route => route.continue({
        url: route.request().url().replace('localhost:8080', 'localhost:8181'),
    }));
    async function get(path) {
        const response = await setup.get(`/api/erp/${path}`);
        assert.equal(response.status(), 200, await response.text());
        return response.json();
    }
    async function create(path, data) {
        const response = await setup.post(`/api/erp/${path}`, { data });
        assert.equal(response.status(), 201, `${path}: ${await response.text()}`);
        return (await response.json()).id;
    }
    async function browserRequest(path, method = 'GET', data) {
        return page.evaluate(async ({ path, method, data }) => {
            const headers = { 'Content-Type': 'application/json' };
            if (method !== 'GET') {
                const token = await fetch('http://localhost:8080/api/auth/csrf', { credentials: 'include' }).then(r => r.json());
                headers[token.headerName] = token.token;
            }
            const response = await fetch(`http://localhost:8080${path}`, {
                method, credentials: 'include', headers, body: data === undefined ? undefined : JSON.stringify(data),
            });
            return { status: response.status, body: await response.text() };
        }, { path, method, data });
    }
    async function login(account, password = account.password) {
        await page.getByLabel('Login', { exact: true }).fill(account.login);
        await page.getByLabel('Password', { exact: true }).fill(password);
        await page.getByRole('button', { name: 'Sign in', exact: true }).click();
    }
    try {
        const suffix = Date.now().toString();
        const org = await create('core/organizations', { name: `Sales access ${suffix}`, nature: 'Company', publicOrganization: false, active: true });
        const otherOrg = await create('core/organizations', { name: `Other organization ${suffix}`, nature: 'Company', publicOrganization: false, active: true });
        const unit = await create('core/units', { organizationId: org, code: `UNIT-${suffix}`, name: 'Allowed unit', type: 'Unit' });
        const otherUnit = await create('core/units', { organizationId: org, code: `OTHER-${suffix}`, name: 'Other unit', type: 'Unit' });
        const buyer = await create('core/people', { personType: 'INDIVIDUAL', fullName: `Buyer ${suffix}`, active: true });
        const locations = [];
        for (const scope of [{ organizationId: org, unitId: unit }, { organizationId: org, unitId: otherUnit }, { organizationId: otherOrg }]) {
            locations.push(await create('inventory/locations', { ...scope, name: `Stock ${locations.length} ${suffix}`, type: 'Warehouse', controlled: true }));
        }
        const brand = await create('inventory/brands', { name: `Brand ${suffix}`, manufacturer: 'Validation' });
        async function model(lotControlled) {
            const category = await create('inventory/categories', { name: `Category ${lotControlled} ${suffix}`, family: 'GENERAL', serialized: !lotControlled, lotControlled, consumable: lotControlled });
            return create('inventory/models', { categoryId: category, brandId: brand, name: `Model ${lotControlled} ${suffix}`, sku: `MODEL-${lotControlled}-${suffix}`, unitOfMeasure: 'EA', listPrice: '12.3456' });
        }
        const assetModel = await model(false);
        const lotModel = await model(true);
        const assetIds = [];
        for (const locationId of locations) {
            assetIds.push(await create('inventory/assets', { modelId: assetModel, locationId, assetCode: `ASSET-${locationId}-${suffix}`, serialNumber: randomUUID(), condition: 'NEW', status: 'AVAILABLE', currentValue: '12.3456' }));
        }
        const lotCode = `LOT-${suffix}`;
        const lotId = await create('inventory/lots', { modelId: lotModel, openingLocationId: locations[0], lotNumber: lotCode, initialQuantity: '10.125' });
        const balance = (await get(`inventory/balances?organizationId=${org}`)).content.find(item => item.lotId === lotId);
        assert.ok(balance);

        const permissions = new Map();
        for (let pageIndex = 0; ; pageIndex++) {
            const result = await get(`core/permissions?page=${pageIndex}&size=100`);
            result.content.forEach(item => permissions.set(`${item.resource}:${item.action}`, item.id));
            if ((pageIndex + 1) * result.size >= result.totalElements) break;
        }
        async function account(name, writes) {
            const personId = await create('core/people', { personType: 'INDIVIDUAL', fullName: `${name} ${suffix}`, active: true });
            const login = `${name.toLowerCase()}-${suffix}`;
            const password = `Validation-${randomUUID()}`;
            const id = await create('core/users', { personId, login, password, blocked: false });
            const profiles = [
                { level: 'ORGANIZATION', grants: [['core/organizations', 'READ']] },
                { level: 'SYSTEM', grants: [['core/people', 'READ'], ['inventory/sale-return-reason-types', 'READ']] },
                { level: 'UNIT', unitId: unit, grants: [['core/units', 'READ'], ['inventory/assets', 'READ'], ['sales', 'READ'],
                    ...(writes ? [['sales', 'CREATE'], ['sales', 'RETURN'], ['sales', 'CANCEL']] : [])] },
            ];
            for (const spec of profiles) {
                const profileId = await create('core/profiles', { name: `${login}-${spec.level}`, level: spec.level });
                for (const [resource, action] of spec.grants) {
                    const key = `${resource}:${action}`;
                    if (!permissions.has(key)) permissions.set(key, await create('core/permissions', { resource, action }));
                    await create('core/profile-permissions', { profileId, permissionId: permissions.get(key) });
                }
                await create('core/user-profiles', { userId: id, profileId, organizationId: org, unitId: spec.unitId });
            }
            return { id, login, password, personId };
        }
        const operator = await account('Operator', true);
        const reader = await account('Reader', false);
        await page.goto(`${appURL}/erp/sales`);
        await page.waitForURL('**/login?returnTo=*');
        assert.equal((await browserRequest('/api/erp/inventory/assets')).status, 401);
        await login(operator, 'Invalid-test-password');
        await page.getByText('Invalid login or password.', { exact: true }).waitFor();
        const oldSession = (await context.cookies()).find(cookie => cookie.name === 'JSESSIONID')?.value;
        await login(operator);
        await page.waitForURL(`${appURL}/erp/sales`);
        const newSession = (await context.cookies()).find(cookie => cookie.name === 'JSESSIONID');
        assert.ok(newSession?.httpOnly);
        assert.notEqual(newSession.value, oldSession);
        console.log('PASS browser login: unauthenticated access denied, invalid password rejected, session rotated');

        const visible = await browserRequest('/api/erp/inventory/assets');
        assert.equal(visible.status, 200, visible.body);
        assert.deepEqual(JSON.parse(visible.body).content.map(item => item.id), [assetIds[0]]);
        for (const id of assetIds.slice(1)) assert.equal((await browserRequest(`/api/erp/inventory/assets/${id}`)).status, 403);
        for (const query of [`organizationId=${org}`, `organizationId=${org}&unitId=${otherUnit}`, `organizationId=${otherOrg}`]) {
            assert.equal((await browserRequest(`/api/erp/sales?${query}`)).status, 403, query);
        }
        assert.equal((await browserRequest('/api/erp/core/users')).status, 403);
        assert.equal((await browserRequest('/api/erp/inventory/assets', 'POST', {})).status, 403);
        console.log('PASS browser permissions: exact unit scope, other organization denied, administration and generic writes denied');

        await page.getByRole('searchbox', { name: 'Search organization', exact: true }).fill(`Sales access ${suffix}`);
        await page.locator('#core-organizationId').selectOption(String(org));
        await page.locator('#core-unitId').selectOption(String(unit));
        await page.getByRole('searchbox', { name: 'Search buyer', exact: true }).fill(`Buyer ${suffix}`);
        await page.locator('#core-buyerId').selectOption(String(buyer));
        await page.locator('#sale-payment').selectOption('PIX');
        await page.getByRole('row').filter({ hasText: `ASSET-${locations[0]}-${suffix}` }).getByRole('button', { name: 'Add', exact: true }).click();
        await page.locator('#stock-kind').selectOption('LOT');
        await page.getByRole('row').filter({ hasText: lotCode }).getByRole('button', { name: 'Add', exact: true }).click();
        await page.getByRole('spinbutton', { name: `Quantity for ${lotCode}`, exact: true }).fill('2.125');
        const saleURL = 'http://localhost:8080/api/erp/sales';
        const finalizeAttempts = [];
        await context.route(saleURL, async route => {
            finalizeAttempts.push(route.request().postDataJSON().requestId);
            if (finalizeAttempts.length === 1) {
                const response = await route.fetch({ url: route.request().url().replace('localhost:8080', 'localhost:8181') });
                assert.equal(response.status(), 200, await response.text());
                await route.abort('failed');
            } else if (finalizeAttempts.length === 2) {
                await route.fulfill({ status: 403, contentType: 'application/problem+json',
                    body: JSON.stringify({ status: 403, detail: 'Finalization retry temporarily denied during validation.' }) });
            } else await route.continue({ url: route.request().url().replace('localhost:8080', 'localhost:8181') });
        });
        await page.getByRole('button', { name: 'Finalize', exact: true }).click();
        await page.getByText('Unable to complete the request.', { exact: false }).waitFor();
        await page.getByRole('button', { name: 'Retry finalization', exact: true }).click();
        await page.getByText('Finalization retry temporarily denied during validation.', { exact: true }).waitFor();
        assert.equal(await page.locator('#sale-payment').isDisabled(), true);
        await page.getByRole('button', { name: 'Retry finalization', exact: true }).click();
        await page.getByText(/Sale #\d+ finalized successfully\./).waitFor();
        assert.equal(new Set(finalizeAttempts).size, 1);
        assert.equal((await get(`sales?organizationId=${org}&unitId=${unit}`)).totalElements, 1);
        await context.unroute(saleURL);
        let sale = (await get(`sales?organizationId=${org}&unitId=${unit}`)).content[0];
        assert.equal(sale.total, '38.5800');
        assert.equal(sale.finalizedByLogin, operator.login);
        assert.equal((await get(`inventory/assets/${assetIds[0]}`)).status, 'SOLD');
        assert.equal((await get(`inventory/balances/${balance.id}`)).available, '8.0000');
        console.log('PASS mixed sale: serialized asset + fractional lot, exact total, stock deduction and operator snapshot');

        const detail = page.locator('details').filter({ has: page.locator('summary').filter({ hasText: `Sale #${sale.id} ·` }) });
        await detail.locator('summary').click();
        const reason = (await get('inventory/sale-return-reason-types')).content.find(item => item.code === 'CUSTOMER_RETURN');
        await detail.locator('select').selectOption(String(reason.id));
        const returnSection = detail.locator('section').filter({ has: page.getByRole('heading', { name: 'Return or cancel sale' }) });
        await returnSection.locator('input').nth(1).fill('Partial lot return after browser validation');
        await returnSection.locator('input').nth(2).fill(`REFUND-${suffix}`);
        await returnSection.getByRole('row').filter({ hasText: lotCode }).getByRole('spinbutton').fill('3');
        await Promise.all([
            page.waitForResponse(response => response.url().endsWith(`/sales/${sale.id}/returns`) && response.status() === 409),
            returnSection.getByRole('button', { name: 'Return selected items', exact: true }).click(),
        ]);
        assert.equal((await get(`sales/${sale.id}`)).returns.length, 0);
        await returnSection.getByRole('row').filter({ hasText: lotCode }).getByRole('spinbutton').fill('1');
        const returnURL = `http://localhost:8080/api/erp/sales/${sale.id}/returns`;
        const dropped = new Set();
        let retryRejected = false;
        const submittedIds = [];
        await context.route(returnURL, async route => {
            const data = route.request().postDataJSON();
            submittedIds.push(data.requestId);
            if (!dropped.has(data.cancellation)) {
                dropped.add(data.cancellation);
                const response = await route.fetch({ url: route.request().url().replace('localhost:8080', 'localhost:8181') });
                assert.equal(response.status(), 200, await response.text());
                await route.abort('failed'); // Commit succeeded; simulate a lost response to the browser.
            } else if (!data.cancellation && !retryRejected) {
                retryRejected = true;
                await route.fulfill({ status: 403, contentType: 'application/problem+json',
                    body: JSON.stringify({ status: 403, detail: 'Retry temporarily denied during validation.' }) });
            } else await route.continue({ url: route.request().url().replace('localhost:8080', 'localhost:8181') });
        });
        await returnSection.getByRole('button', { name: 'Return selected items', exact: true }).click();
        await returnSection.getByText('Unable to complete the request.', { exact: false }).waitFor();
        assert.equal((await get(`sales/${sale.id}`)).returns.length, 1);
        assert.equal(await returnSection.getByRole('button', { name: 'Cancel remaining sale', exact: true }).isDisabled(), true);
        assert.equal(await returnSection.locator('input').nth(1).isDisabled(), true);
        await returnSection.getByRole('button', { name: 'Retry return', exact: true }).click();
        await returnSection.getByText('Retry temporarily denied during validation.', { exact: true }).waitFor();
        assert.equal(await returnSection.locator('input').nth(1).isDisabled(), true);
        await Promise.all([
            page.waitForResponse(response => response.url().endsWith(`/sales/${sale.id}/returns`) && response.status() === 200),
            returnSection.getByRole('button', { name: /^(Return selected items|Retry return)$/ }).click(),
        ]);
        sale = await get(`sales/${sale.id}`);
        assert.equal(sale.returns.length, 1, 'Retry after a lost response must not create a second stock return');
        assert.equal(new Set(submittedIds).size, 1, 'Every retry must retain the original request identifier');
        assert.equal(sale.returns[0].refundAmount, '12.3456');
        assert.equal((await get(`inventory/balances/${balance.id}`)).available, '9.0000');
        await returnSection.getByRole('row').filter({ hasText: lotCode }).getByText('1.1250 EA', { exact: true }).waitFor();
        await returnSection.getByRole('button', { name: 'Cancel remaining sale', exact: true }).click();
        await returnSection.getByText('Unable to complete the request.', { exact: false }).waitFor();
        assert.equal((await get(`sales/${sale.id}`)).returns.length, 2);
        assert.equal(await returnSection.getByRole('button', { name: 'Return selected items', exact: true }).isDisabled(), true);
        await returnSection.getByRole('button', { name: 'Retry cancellation', exact: true }).click();
        await page.getByText('All sale items have been returned.', { exact: true }).waitFor();
        sale = await get(`sales/${sale.id}`);
        assert.equal(sale.returns.length, 2);
        assert.equal(sale.returns[1].refundAmount, '26.2344');
        assert.equal(sale.returns[1].cancellation, true);
        assert.equal((await get(`inventory/balances/${balance.id}`)).available, '10.1250');
        assert.equal((await get(`inventory/assets/${assetIds[0]}`)).status, 'AVAILABLE');
        assert.equal(submittedIds.at(-1), submittedIds.at(-2));
        await context.unroute(returnURL);
        console.log('PASS partial return and cancellation: validation error, lost responses and rejected retry preserve stock and request identity');

        await page.getByRole('button', { name: 'Open user menu', exact: true }).click();
        await page.getByRole('menuitem', { name: 'Sign out', exact: true }).click();
        await page.waitForURL(url => url.pathname === '/login');
        assert.equal((await browserRequest('/api/erp/inventory/assets')).status, 401);
        await login(reader);
        await page.waitForURL(url => url.pathname !== '/login');
        await page.goto(`${appURL}/erp/inventory`);
        await page.getByRole('navigation', { name: 'Assets and inventory resources', exact: true }).getByRole('button', { name: 'Individual assets', exact: true }).click();
        await page.getByText(`ASSET-${locations[0]}-${suffix}`, { exact: false }).first().waitFor();
        assert.equal(await page.getByRole('button', { name: 'New', exact: true }).count(), 0);
        assert.equal(await page.getByText(`ASSET-${locations[1]}-${suffix}`, { exact: true }).count(), 0);
        const deniedSale = await browserRequest('/api/erp/sales', 'POST', {
            requestId: randomUUID(), organizationId: org, unitId: unit, buyerId: buyer, paymentMethod: 'PIX',
            items: [{ assetId: assetIds[0], quantity: '1', expectedUnitPrice: '12.3456' }],
        });
        assert.equal(deniedSale.status, 403, deniedSale.body);
        assert.equal((await get(`sales?organizationId=${org}&unitId=${unit}`)).totalElements, 1);
        const readerRecord = await get(`core/users/${reader.id}`);
        const blocked = await setup.put(`/api/erp/core/users/${reader.id}`, { data: {
            personId: reader.personId, login: reader.login, version: readerRecord.version, blocked: true,
        } });
        assert.equal(blocked.status(), 200, await blocked.text());
        await page.reload();
        await page.waitForURL('**/login?returnTo=*');
        await login(reader);
        await page.getByText('Invalid login or password.', { exact: true }).waitFor();
        assert.equal((await browserRequest('/api/erp/inventory/assets')).status, 401);
        console.log('PASS logout, read-only interface, blocked account invalidates its existing browser session');
        assert.deepEqual(errors, []);
        console.log(JSON.stringify({ organizationId: org, unitId: unit, saleId: sale.id, operatorId: operator.id, readerId: reader.id, result: 'PASS' }));
    } catch (error) {
        await page.screenshot({ path: '../wr-api/target/browser-validation/sales-access-failure.png', fullPage: true }).catch(() => {});
        throw error;
    } finally {
        await browser.close();
        await setup.dispose();
    }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
