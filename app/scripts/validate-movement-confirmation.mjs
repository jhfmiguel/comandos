import assert from 'node:assert/strict';
import { chromium } from 'playwright';

// Frontend contract tests: every API call is intercepted; no real stock is changed.
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const pageData = content => ({ content, totalElements: content.length, page: 0, size: 20 });
const stock = { kind: 'ASSET', stockId: 10, balanceId: 20, code: 'ASSET-10', lotNumber: 'LOT-20',
    sku: 'SKU', modelName: 'Equipment', locationName: 'Source warehouse', available: '10', unitOfMeasure: 'EA' };
try {
    for (const scenario of ['donations', 'ammunition-consumptions', 'disposals', 'destruction', 'APPROVED', 'REJECTED']) {
        const maintenance = ['APPROVED', 'REJECTED'].includes(scenario);
        const resource = maintenance ? 'maintenance' : scenario === 'destruction' ? 'disposals' : scenario;
        const context = await browser.newContext();
        await context.addInitScript(() => localStorage.setItem('comandos-locale', 'en-US'));
        const page = await context.newPage();
        page.setDefaultTimeout(15000);
        const errors = [], attempts = [];
        page.on('pageerror', error => errors.push(error.message));
        let completed;
        const order = { id: 1, organizationId: 1, organizationName: 'Organization One', unitName: 'Unit One',
            assetCode: 'ASSET-10', modelName: 'Equipment', locationName: 'Source warehouse', reason: 'Repair', status: 'OPEN', services: [] };
        await context.route('**/api/**', async route => {
            const request = route.request(), url = new URL(request.url()), path = url.pathname;
            const json = data => route.fulfill({ json: data });
            if (path.endsWith('/auth/session')) return json({ requireLogin: false, user: null, access: { enforced: false, grants: [] } });
            if (path.endsWith('/auth/csrf')) return json({ headerName: 'X-CSRF-TOKEN', token: 'fixture' });
            if (request.method() === 'POST') {
                attempts.push(request.postDataJSON());
                if (attempts.length === 1) return route.abort('failed');
                if (attempts.length === 2) return route.fulfill({ status: 403, json: { detail: 'Recovery temporarily denied.' } });
                completed = { ...order, ...attempts[0], status: maintenance ? 'COMPLETED' : 'FINALIZED', id: 1,
                    donorName: 'Person One', doneeName: 'Person Two', responsibleName: 'Person One', authorizerName: 'Person Two',
                    finalizedAt: '2026-09-20T10:00:00', consumedAt: '2026-09-20T10:00:00',
                    items: (attempts[0].items || []).map((item, id) => ({ ...stock, ...item, id, stockCode: stock.code })) };
                return json(completed);
            }
            if (path.endsWith('/organizations')) return json(pageData([{ id: 1, label: 'Organization One' }]));
            if (path.endsWith('/units')) return json(pageData([{ id: 2, organizationId: 1, label: 'Unit One' }]));
            if (path.endsWith('/people')) return json(pageData([{ id: 3, label: 'Person One' }, { id: 4, label: 'Person Two' }]));
            if (path.endsWith('/stock')) return json(pageData([{ ...stock, kind: url.searchParams.get('kind') || 'LOT' }]));
            if (path.endsWith('/orders')) return json(pageData([completed || order]));
            return json(pageData(completed && path.endsWith(resource) ? [completed] : []));
        });
        const routeName = resource === 'ammunition-consumptions' ? 'ammunition-consumption' : resource;
        await page.goto(`${process.env.APP_URL || 'http://localhost:3000'}/erp/${routeName}`);
        await page.locator(`#core-${maintenance ? 'organization' : 'organizationId'}`).selectOption('1');
        await page.locator(`#core-${maintenance ? 'unit' : 'unitId'}`).selectOption('2');
        let button, retry;
        if (maintenance) {
            await page.locator('summary').filter({ hasText: 'ASSET-10' }).click();
            for (const field of ['defect', 'cause', 'opinion', 'service', 'notes']) await page.locator(`#${field}-1`).fill(`${field} reviewed`);
            await page.locator('#result-1').selectOption(scenario);
            button = 'Complete work order'; retry = 'Retry completion';
        } else {
            if (resource === 'donations') {
                await page.locator('#core-donorId').selectOption('3');
                await page.locator('#core-doneeId').selectOption('4');
                await page.locator('#donation-term').fill('Signed donation term');
                button = 'Finalize donation';
            } else if (resource === 'disposals') {
                await page.locator('#disposal-process').fill('PROCESS-1');
                await page.locator('#disposal-reason').fill('Unserviceable');
                if (scenario === 'destruction') {
                    await page.getByRole('checkbox').check();
                    await page.locator('#destruction-method').fill('Certified destruction');
                    await page.locator('#destroyed-at').fill('2026-09-20T10:00');
                    await page.locator('#destruction-certificate').fill('CERT-1');
                }
                button = 'Finalize disposal';
            } else {
                await page.locator('#core-responsibleId').selectOption('3');
                await page.locator('#core-authorizerId').selectOption('4');
                await page.locator('#consumption-purpose').fill('Training');
                button = 'Finalize consumption';
            }
            await page.getByRole('button', { name: 'Add', exact: true }).click();
            if (resource !== 'ammunition-consumptions') {
                await page.locator(resource === 'donations' ? '#donation-kind' : '#disposal-kind').selectOption('LOT');
                await page.getByRole('button', { name: 'Add', exact: true }).click();
                await page.getByRole('spinbutton', { name: 'Quantity for ASSET-10', exact: true }).last().fill('2.5');
            }
            retry = 'Retry finalize';
        }
        const submit = page.getByRole('button', { name: button, exact: true });
        page.once('dialog', async dialog => {
            assert.match(dialog.message(), /Source warehouse/);
            assert.match(dialog.message(), /Result:/);
            assert.match(dialog.message(), /Organization One/);
            if (resource === 'donations') assert.match(dialog.message(), /Person Two/);
            if (scenario === 'destruction') assert.match(dialog.message(), /CERT-1/);
            await dialog.dismiss();
        });
        await submit.click();
        assert.equal(attempts.length, 0, 'Cancel must not submit');
        page.once('dialog', dialog => dialog.accept());
        await submit.evaluate(button => { button.form.requestSubmit(); button.form.requestSubmit(); });
        await page.getByText('The result could not be confirmed.', { exact: false }).waitFor();
        assert.equal(await page.getByRole('button', { name: 'Add', exact: true }).count(), 0);
        const form = maintenance ? page.locator('form').filter({ has: page.locator('#defect-1') }) : page.locator('form').first();
        assert.equal(await form.locator('fieldset input').first().isDisabled(), true);
        await page.getByRole('button', { name: retry, exact: true }).click();
        await page.getByText('Recovery temporarily denied.', { exact: true }).waitFor();
        assert.equal(await form.locator('fieldset input').first().isDisabled(), true);
        await page.getByRole('button', { name: retry, exact: true }).click();
        await page.getByText(maintenance ? 'Work order #1 completed.' : /#1 finalized successfully\./).waitFor();
        await page.locator('summary').filter({ hasText: maintenance ? 'COMPLETED' : 'FINALIZED' }).waitFor();
        if (maintenance) await page.getByText(`Functional test: ${scenario}`, { exact: false }).waitFor();
        assert.equal(attempts.length, 3);
        assert.deepEqual(attempts[0], attempts[1]);
        assert.deepEqual(attempts[0], attempts[2]);
        assert.deepEqual(errors, []);
        console.log(`PASS ${scenario}: review, cancel, response loss, denied retry, identical recovery, history`);
        await context.close();
    }
    for (const recipientType of ['PERSON', 'UNIT']) {
        const context = await browser.newContext();
        await context.addInitScript(() => localStorage.setItem('comandos-locale', 'en-US'));
        const page = await context.newPage();
        const issued = [], returned = [], errors = [];
        let custody;
        page.on('pageerror', error => errors.push(error.message));
        await context.route('**/api/**', async route => {
            const request = route.request(), path = new URL(request.url()).pathname;
            const json = data => route.fulfill({ json: data });
            if (path.endsWith('/auth/session')) return json({ requireLogin: false, user: null });
            if (path.endsWith('/auth/csrf')) return json({ headerName: 'X-CSRF-TOKEN', token: 'fixture' });
            if (path.endsWith('/returns')) {
                returned.push(request.postDataJSON());
                if (returned.length === 1) return route.abort('failed');
                if (returned.length === 2) return route.fulfill({ status: 403, json: { detail: 'Recovery temporarily denied.' } });
                custody.items = custody.items.map(item => ({ ...item, returnedAt: '2026-09-20T11:00:00' }));
                return json(custody);
            }
            if (request.method() === 'POST') {
                issued.push(request.postDataJSON());
                custody = { ...issued[0], id: 1, recipientType, recipientName: recipientType === 'PERSON' ? 'Person One' : 'Unit One',
                    organizationName: 'Organization One', unitName: 'Unit One', authorizerName: 'Person One', status: 'ACTIVE',
                    deliveredAt: '2026-09-20T10:00:00', items: [1, 2].map(id => ({ id, assetCode: `ASSET-${id}`, modelName: 'Equipment',
                        equipmentSetId: 7, equipmentSetCode: 'KIT-7', componentRole: 'Component', quantity: 1, locationName: 'Source warehouse' })) };
                return json(custody);
            }
            if (path.endsWith('/organizations')) return json(pageData([{ id: 1, label: 'Organization One' }]));
            if (path.endsWith('/units')) return json(pageData([{ id: 2, organizationId: 1, label: 'Unit One' }]));
            if (path.endsWith('/people')) return json(pageData([{ id: 3, label: 'Person One' }]));
            if (path.endsWith('/equipment-sets')) return json(pageData([{ equipmentSetId: 7, code: 'KIT-7', name: 'Complete kit', componentCount: 2 }]));
            if (path.endsWith('/custody-return-condition-types')) return json(pageData([{ id: 5, label: 'Good', active: true }]));
            return json(pageData(path.endsWith('/custodies') && custody ? [custody] : []));
        });
        await page.goto(`${process.env.APP_URL || 'http://localhost:3000'}/erp/custody`);
        await page.locator('#core-organizationId').selectOption('1');
        await page.locator('#core-unitId').selectOption('2');
        await page.locator('#custody-recipient-type').selectOption(recipientType);
        await page.locator(recipientType === 'PERSON' ? '#core-recipientId' : '#core-recipientUnitId').selectOption(recipientType === 'PERSON' ? '3' : '2');
        await page.locator('#core-authorizerId').selectOption('3');
        await page.locator('#custody-purpose').fill('Kit assignment');
        await page.getByRole('button', { name: 'Add set', exact: true }).click();
        await page.getByRole('button', { name: 'Issue custody', exact: true }).click();
        await page.getByText('Custody #1 issued successfully.').waitFor();
        assert.deepEqual(issued[0].equipmentSetIds, [7]);
        assert.equal(issued[0][recipientType === 'PERSON' ? 'recipientId' : 'recipientUnitId'], recipientType === 'PERSON' ? 3 : 2);
        await page.locator('summary').filter({ hasText: 'Custody #1' }).click();
        await page.locator('#core-returnConditionTypeId').selectOption('5');
        await page.getByRole('button', { name: 'Return set', exact: true }).first().click();
        await page.getByRole('button', { name: 'Retry pending return' }).click();
        await page.getByText('Recovery temporarily denied.').waitFor();
        await page.getByRole('button', { name: 'Retry pending return' }).click();
        await page.getByText('Returned 2026-09-20 11:00:00').first().waitFor();
        assert.equal(returned.length, 3);
        assert.deepEqual(returned[0].itemIds, [1, 2]);
        assert.deepEqual(returned[0], returned[1]);
        assert.deepEqual(returned[0], returned[2]);
        assert.deepEqual(errors, []);
        console.log(`PASS kit ${recipientType}: issue, history, full set return and identical retry after response loss and 403`);
        await context.close();
    }
} finally { await browser.close(); }
