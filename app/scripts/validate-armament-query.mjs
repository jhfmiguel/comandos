import assert from 'node:assert/strict';
import { chromium } from '@playwright/test';

const browser = await chromium.launch({ channel: 'msedge', headless: true });
const origin = process.env.APP_URL || 'http://localhost:3000';
const asset = { id: 12, assetCode: 'ARM-12', serialNumber: 'SER-12', status: 'CUSTODIED', condition: 'GOOD',
    modelId: 1, locationId: 2, unitId: 3, organizationId: 4,
    referenceLabels: { modelId: 'Modelo Alfa', locationId: 'Reserva', unitId: 'Unidade Alfa', organizationId: 'Org' } };
try {
    for (const scenario of ['success', 'empty', 'error', 'forbidden', 'restricted']) {
        const context = await browser.newContext();
        const page = await context.newPage();
        page.setDefaultTimeout(15000);
        console.log(`Running ${scenario}`);
        const requests = [], errors = [];
        let fail = scenario === 'error';
        page.on('pageerror', e => errors.push(e.message));
        await context.route('**/api/**', async route => {
            const url = new URL(route.request().url()), path = url.pathname;
            requests.push(url);
            const json = data => route.fulfill({ json: data });
            const paged = content => json({ content, page: Number(url.searchParams.get('page') || 0), size: 10, totalElements: content.length });
            if (path.endsWith('/auth/session')) return json({ requireLogin: false, user: null, access: {
                enforced: scenario === 'restricted', grants: [{ resource: 'inventory/assets', action: 'READ', scope: 'UNIT', organizationId: 4, unitId: 3 }]
            } });
            if (path.endsWith('/inventory/assets')) {
                await new Promise(resolve => setTimeout(resolve, 300));
                if (fail || scenario === 'forbidden') return route.fulfill({ status: fail ? 500 : 403, json: {} });
                return json({ content: scenario === 'empty' ? [] : [asset], page: Number(url.searchParams.get('page') || 0), size: 10, totalElements: scenario === 'empty' ? 0 : 21 });
            }
            if (path.endsWith('/inventory/assets/12')) return json(asset);
            if (path.endsWith('/custodies/by-asset/12')) return paged([{ id: 8, recipientName: 'Responsável Alfa', status: 'ACTIVE', deliveredAt: '2026-09-20', items: [{ id: 9, assetId: 12, returnedAt: null }] }]);
            if (path.endsWith('/inventory/movements')) return paged([{ id: 5, nature: 'CUSTODY_ISSUE', movedAt: '2026-09-20', referenceLabels: { locationId: 'Reserva' } }]);
            if (path.endsWith('/audit')) return paged([{ id: 6, action: 'UPDATE', occurredAt: '2026-09-20', actorLogin: 'operador' }]);
            return paged([]);
        });
        await page.goto(`${origin}/queries/weapons?assetCode=ARM-12&serialNumber=SER-12&modelId=Alfa&status=CUSTODIED&unit=Alfa&locationId=Reserva&page=1`);
        const statusOptions = await page.locator('#query-status option').evaluateAll(options =>
            options.map(option => option.value).filter(Boolean)
        );
        assert.deepEqual(statusOptions, [
            'DRAFT',
            'AVAILABLE',
            'BLOCKED',
            'CUSTODIED',
            'IN_MAINTENANCE',
            'TRANSFER_PENDING',
            'MISSING',
            'RESTRICTED',
            'SOLD',
            'DONATED',
            'DISPOSED'
        ]);
        if (scenario === 'error' || scenario === 'forbidden') {
            await page.getByText(scenario === 'error' ? 'Não foi possível consultar os dados. Tente novamente.' : 'Sem autorização para consultar estes dados.').waitFor();
            if (scenario === 'error') { fail = false; await page.getByRole('button', { name: 'Tentar novamente' }).click(); await page.getByRole('button', { name: 'Ver detalhe' }).waitFor(); }
        } else if (scenario === 'empty') {
            await page.getByText('Nenhum armamento encontrado.').waitFor();
        } else {
            await page.getByRole('button', { name: 'Ver detalhe' }).click();
            await page.getByRole('heading', { name: 'Armamento ARM-12' }).waitFor();
            assert.equal(new URL(page.url()).searchParams.get('asset'), '12');
            if (scenario === 'success') {
                await page.getByText('Responsável Alfa', { exact: false }).waitFor();
                await page.getByText('CUSTODY_ISSUE', { exact: false }).waitFor();
                await page.getByText('operador', { exact: false }).waitFor();
                assert(requests.some(url => url.pathname.endsWith('/movements') && url.searchParams.get('assetId') === '12'));
                assert(requests.some(url => url.pathname.endsWith('/audit') && url.searchParams.get('recordId') === '12'));
                await page.reload();
                await page.getByRole('heading', { name: 'Armamento ARM-12' }).waitFor();
                assert.equal(await page.getByLabel('Patrimônio', { exact: true }).inputValue(), 'ARM-12');
                await page.getByText('Página 2 · 21 registros').waitFor();
                await page.getByRole('navigation', { name: 'Paginação' }).first().getByRole('button', { name: 'Próxima' }).click();
                await page.getByText('Página 3 · 21 registros').waitFor();
                await page.goBack();
                await page.getByRole('heading', { name: 'Armamento ARM-12' }).waitFor();
                await page.getByLabel('Patrimônio', { exact: true }).fill('ARM-13');
                await page.getByRole('button', { name: 'Consultar', exact: true }).click();
                await page.getByText('Página 1 · 21 registros').waitFor();
                assert.equal(new URL(page.url()).searchParams.get('page'), null);
                assert.equal(new URL(page.url()).searchParams.get('asset'), null);
            } else {
                assert.equal(await page.getByText('Sem permissão para consultar esta seção.').count(), 3);
                assert(!requests.some(url => /by-asset|movements|\/audit$/.test(url.pathname)));
            }
        }
        const request = requests.find(url => url.pathname.endsWith('/inventory/assets'));
        for (const [key, expected] of Object.entries({ assetCode: 'ARM-12', serialNumber: 'SER-12', modelId: 'Alfa', status: 'CUSTODIED', unit: 'Alfa', locationId: 'Reserva' })) assert.equal(request.searchParams.get(`filter.${key}`), expected);
        assert.equal(request.searchParams.get('page'), '1');
        assert.equal(request.searchParams.get('size'), '10');
        assert.deepEqual(errors, []);
        await context.close();
        console.log(`PASS ${scenario}`);
    }
} finally { await browser.close(); }
