import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const { chromium, request } = createRequire(import.meta.url)('playwright');

// Isolated validation API; never point this script at the primary database.
const apiUrl = process.env.ASSET_BATCH_API_URL ?? 'http://localhost:8180';
const appUrl = process.env.ASSET_BATCH_APP_URL ?? 'http://localhost:3000';

const api = await request.newContext({ baseURL: apiUrl });
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const context = await browser.newContext();
await context.addInitScript(() => localStorage.setItem('comandos-locale', 'en-US'));
const page = await context.newPage();

page.setDefaultTimeout(20000);

const errors = [];
let retryPath = '';
let retryBodies = [];

const traceNetwork = process.env.ASSET_BATCH_TRACE === '1';
const relevantRequest = request => /\/api\/(auth\/csrf|erp\/inventory\/assets\/batch)(?:[/?]|$)/.test(request.url());
page.on('pageerror', error => {
    errors.push(error.message);
    console.error('[pageerror]', error.message);
});
page.on('request', request => {
    if (traceNetwork && relevantRequest(request)) {
        console.log('[request]', request.method(), request.url(), request.postData() ?? '');
    }
});
page.on('response', async response => {
    if (traceNetwork && relevantRequest(response.request())) {
        const body = await response.text().catch(error => `<unavailable: ${error.message}>`);
        // Do not print CSRF token values.
        console.log('[response]', response.request().method(), response.url(), response.status(),
            new URL(response.url()).pathname === '/api/auth/csrf' ? '<CSRF body omitted>' : body);
    }
});
page.on('requestfailed', request => {
    if (traceNetwork && relevantRequest(request)) {
        console.log('[requestfailed]', request.method(), request.url(), request.failure()?.errorText);
    }
});

await context.route('http://localhost:8080/api/**', async route => {
    const url = route.request().url().replace('http://localhost:8080', apiUrl);
    if (traceNetwork && relevantRequest(route.request())) {
        console.log('[route]', route.request().method(), route.request().url(), '->', url);
    }

    if (retryPath && new URL(url).pathname === retryPath && route.request().method() === 'POST') {
        retryBodies.push(route.request().postDataJSON());

        if (retryBodies.length === 1) {
            const committed = await route.fetch({ url });
            assert.equal(committed.status(), 200, await committed.text());
            await route.abort('failed');
            return;
        }

        if (retryBodies.length === 2) {
            await route.fulfill({
                status: 403,
                contentType: 'application/problem+json',
                body: JSON.stringify({
                    detail: 'Validation retry permission failure'
                })
            });
            return;
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
    assert.equal(response.status(), 200, await response.text());
    return response.json();
}

async function choose(labels, id, name) {
    const candidates = Array.isArray(labels) ? labels : [labels];

    const technicalLabel = candidates[0];

    await page.getByRole('searchbox', {
        name: `Search ${technicalLabel.toLowerCase()}`,
        exact: true
    }).fill(name);

    for (const label of candidates) {
        const select = page.getByLabel(`${label} *`, { exact: true });

        if (await select.count()) {
            await select.selectOption(String(id));
            return;
        }
    }

    throw new Error(
        `Reference field not found: ${candidates.join(' / ')}`
    );
}

async function recover() {
    await page.getByRole('button', {
        name: 'Retry entry',
        exact: true
    }).click();

    await page.getByText(
        'Validation retry permission failure',
        { exact: true }
    ).waitFor();

    const recovered = page.waitForResponse(response =>
        new URL(response.url()).pathname === retryPath &&
        response.status() === 200
    );

    await page.getByRole('button', {
        name: 'Retry entry',
        exact: true
    }).click();

    await recovered;

    assert.equal(retryBodies.length, 3);
    assert.deepEqual(retryBodies[0], retryBodies[1]);
    assert.deepEqual(retryBodies[0], retryBodies[2]);

    retryPath = '';
}

async function reviewBatch() {
    const responsePromise = page.waitForResponse(response =>
        new URL(response.url()).pathname === '/api/erp/inventory/assets/batch/review' &&
        response.request().method() === 'POST'
    );
    await page.getByRole('button', { name: 'Register assets', exact: true }).click();
    const response = await responsePromise;
    const body = await response.text();
    assert.equal(response.status(), 200, `${response.request().method()} ${response.url()}: ${body}`);
    assert.equal(JSON.parse(body).accepted, true, body);
    await page.getByRole('button', { name: 'Confirm registration', exact: true }).waitFor();
}

try {
    const suffix = Date.now().toString();

    const organizationName = `Intake organization ${suffix}`;

    const organizationId = await create(
        'core/organizations',
        {
            name: organizationName,
            nature: 'Public safety',
            publicOrganization: true,
            active: true
        }
    );

    const unitName = `Issuing unit ${suffix}`;

    const unitId = await create(
        'core/units',
        {
            organizationId,
            code: `ISS-${suffix}`,
            name: unitName,
            type: 'Unit'
        }
    );

    const locationName = `Intake location ${suffix}`;

    const locationId = await create(
        'inventory/locations',
        {
            organizationId,
            unitId,
            name: locationName,
            type: 'Warehouse',
            controlled: true
        }
    );

    const brandId = await create(
        'inventory/brands',
        {
            name: `Intake brand ${suffix}`,
            manufacturer: 'Validation'
        }
    );

    const serializedCategory = await create(
        'inventory/categories',
        {
            name: `Serialized ${suffix}`,
            family: 'OPTICAL',
            serialized: true,
            lotControlled: false,
            consumable: false
        }
    );

    const modelName = `Individual model ${suffix}`;

    const modelId = await create(
        'inventory/models',
        {
            name: modelName,
            categoryId: serializedCategory,
            brandId,
            unitOfMeasure: 'EA',
            sku: `IND-${suffix}`,
            listPrice: '10'
        }
    );

    await page.goto(
        `${appUrl}/erp/inventory?resource=assets`
    );

    await page.getByRole('button', {
        name: /^(New record|Novo registro)$/,
        exact: true
    }).click();

    await choose(
        ['Model', 'Modelo'],
        modelId,
        modelName
    );

    await choose(
        ['Location', 'Localização'],
        locationId,
        locationName
    );

    await page.getByText(
        /^(Paste a list from a spreadsheet|Cole uma lista de uma planilha)$/
    ).click();

    await page.locator('#asset-pairs-paste').fill(
        `CODE-A-${suffix}\tSN-A-${suffix}\n` +
        `CODE-B-${suffix}\tSN-B-${suffix}`
    );

    await page.getByRole('button', {
        name: /^(Add pasted rows|Adicionar linhas coladas)$/
    }).click();

    await page.getByText(
        'Quantity (serial numbers): 2',
        { exact: true }
    ).waitFor();

    await page.getByLabel(
        'Serial number 2',
        { exact: true }
    ).fill(`SN-A-${suffix}`);

    assert.ok(
        await page.getByRole('button', {
            name: 'Register assets',
            exact: true
        }).isDisabled()
    );

    await page.getByLabel(
        'Serial number 2',
        { exact: true }
    ).fill(`SN-B-${suffix}`);

    /*
     * REVIEW
     *
     * The first submission validates the complete batch but
     * must not persist any asset.
     */
    await reviewBatch();

    assert.equal(
        (
            await get(
                `inventory/assets?organizationId=${organizationId}`
            )
        ).content.length,
        0
    );

    /*
     * Return to edition and verify that the reviewed batch
     * preserved the original data.
     */
    await page.getByRole('button', {
        name: 'Edit batch',
        exact: true
    }).click();

    assert.equal(
        await page.getByLabel(
            'Asset code 1',
            { exact: true }
        ).inputValue(),
        `CODE-A-${suffix}`
    );

    /*
     * Review again after editing.
     */
    await reviewBatch();

    /*
     * From this point forward we test idempotent recovery of
     * the actual persistence endpoint.
     */
    retryPath = '/api/erp/inventory/assets/batch';
    retryBodies = [];

    await page.getByRole('button', {
        name: 'Confirm registration',
        exact: true
    }).click();

    await page.getByRole('button', {
        name: 'Retry entry',
        exact: true
    }).waitFor();

    assert.ok(
        await page.getByLabel(
            'Asset code 1',
            { exact: true }
        ).isDisabled()
    );

    await recover();

    await page.getByText(
        '2 individual assets registered successfully.',
        { exact: true }
    ).waitFor();

    assert.equal(
        await page.getByRole('cell', {
            name: 'Accepted; saved',
            exact: true
        }).count(),
        2
    );

    await page.getByRole('button', {
        name: 'Done',
        exact: true
    }).click();

    const assets = (
        await get(
            `inventory/assets?organizationId=${organizationId}`
        )
    ).content;

    assert.equal(assets.length, 2);

    assert.deepEqual(
        assets.map(asset => [
            asset.assetCode,
            asset.serialNumber
        ]),
        [
            [`CODE-A-${suffix}`, `SN-A-${suffix}`],
            [`CODE-B-${suffix}`, `SN-B-${suffix}`]
        ]
    );

    console.log(
        'PASS asset pairs, serial count, review, duplicate validation and identical retries'
    );

    /*
     * Existing-row rejection followed by correction.
     */
    await page.getByRole('button', {
        name: /^(New record|Novo registro)$/,
        exact: true
    }).click();

    await choose(
        ['Model', 'Modelo'],
        modelId,
        modelName
    );

    await choose(
        ['Location', 'Localização'],
        locationId,
        locationName
    );

    await page.getByLabel(
        'Asset code 1',
        { exact: true }
    ).fill(`CODE-A-${suffix}`);

    await page.getByLabel(
        'Serial number 1',
        { exact: true }
    ).fill(`SN-A-${suffix}`);

    await page.getByRole('button', {
        name: /^(Add row|Adicionar linha)$/
    }).click();

    await page.getByLabel(
        'Asset code 2',
        { exact: true }
    ).fill(`CODE-C-${suffix}`);

    await page.getByLabel(
        'Serial number 2',
        { exact: true }
    ).fill(`SN-C-${suffix}`);

    /*
     * The review endpoint itself reports the already registered
     * first row. Nothing may be persisted.
     */
    await page.getByRole('button', {
        name: 'Register assets',
        exact: true
    }).click();

    await page.getByRole('cell', {
        name: /^Asset code is already registered\. Individual asset \(#\d+\)\. Serial number is already registered\. Individual asset \(#\d+\)\.$/
    }).waitFor();

    assert.equal(
        await page.getByLabel(
            'Asset code 2',
            { exact: true }
        ).inputValue(),
        `CODE-C-${suffix}`
    );

    assert.equal(
        (
            await get(
                `inventory/assets?organizationId=${organizationId}`
            )
        ).content.length,
        2
    );

    await page.getByRole('button', {
        name: 'Remove row 1',
        exact: true
    }).click();
    await reviewBatch();

    await page.getByRole('button', {
        name: 'Confirm registration',
        exact: true
    }).click();

    await page.getByText(
        '1 individual assets registered successfully.',
        { exact: true }
    ).waitFor();

    assert.equal(
        (
            await get(
                `inventory/assets?organizationId=${organizationId}`
            )
        ).content.length,
        3
    );

    assert.deepEqual(errors, []);

    console.log(
        'PASS per-row rejections, preserved valid pair, removal and corrected confirmation'
    );
} catch (error) {
    await page.screenshot({
        path:
            'asset-batch-failure.png',
        fullPage: true
    }).catch(screenshotError => console.error('[screenshot]', screenshotError.message));

    throw error;
} finally {
    await browser.close();
    await api.dispose();
}
