import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
const { chromium, request } = createRequire(import.meta.url)('playwright');

// Run only against the separate validation API/database documented in docs/validation.md.
const apiURL = 'http://localhost:8180';
const appURL = 'http://localhost:3000';

async function main() {
    const api = await request.newContext({ baseURL: apiURL });
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(15000);
    const errors = [];
    let delayedSearch;
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', message => {
        if (message.type() === 'error') errors.push(`Console: ${message.text()} (${message.location().url})`);
    });
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

        const coreCatalog = await get('core/catalog');
        for (const resourceKey of [
            'organization-natures',
            'economic-activities',
            'unit-types',
            'person-types',
            'contact-types',
            'profile-levels',
            'permission-resources',
            'permission-actions'
        ]) {
            assert.ok(
                coreCatalog.some(resource => resource.key === resourceKey),
                `core catalog must expose ${resourceKey}`
            );
        }
        console.log('PASS core catalog: all parameter registrations are exposed by the API');

        const organizationNatureId = await create('core/organization-natures', {
            code: `BROWSER-NATURE-${suffix}`,
            name: 'Public safety',
            active: true
        });
        const economicActivityId = await create('core/economic-activities', {
            code: `BROWSER-ACTIVITY-${suffix}`,
            description: 'Public safety administration',
            active: true
        });
        const organizationId = await create('core/organizations', {
            name: `Browser validation ${suffix}`,
            natureId: organizationNatureId,
            economicActivityId,
            publicOrganization: true,
            active: true
        });
        const unitTypeId = await create('core/unit-types', {
            code: `BROWSER_UNIT_TYPE_${suffix}`,
            name: `Browser unit type ${suffix}`,
            description: 'Browser validation organizational unit type',
            active: true
        });
        const unitId = await create('core/units', {
            organizationId,
            code: `UNIT-${suffix}`,
            name: 'Validation unit',
            unitTypeId,
            active: true
        });
        assert.equal(
            (await get(`core/units/${unitId}`)).unitTypeId,
            unitTypeId
        );
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
        await page.getByRole('searchbox', { name: 'Search organization', exact: true }).fill(`Browser validation ${suffix}`);
        await page.locator('#core-organizationId').selectOption(String(organizationId));
        await page.locator('#core-unitId').selectOption(String(unitId));
        await page.getByRole('searchbox', { name: 'Search recipient', exact: true }).fill(`Recipient ${suffix}`);
        await page.locator('#core-recipientId').selectOption(String(recipientId));
        await page.getByRole('searchbox', { name: 'Search authorizer', exact: true }).fill(`Authorizer ${suffix}`);
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
        await page.waitForFunction(({ organizationId, assetId }) =>
            document.querySelector('#core-organization')?.value === String(organizationId)
            && document.querySelector('#maintenance-asset')?.value === String(assetId),
        { organizationId, assetId });
        await page.getByRole('button', { name: 'Open work order', exact: true }).click();
        await page.getByText(/Work order #\d+ opened successfully\./).waitFor();
        assert.equal((await get(`inventory/assets/${assetId}`)).status, 'IN_MAINTENANCE');
        console.log('PASS PostgreSQL/browser: custody issue -> damaged return -> linked maintenance');

        await page.goto(`${appURL}/erp/core?section=institutional&resource=organization-natures`);
        await page.locator('.comandos-tab-panel #resource-title').waitFor();
        assert.match(await page.locator('.comandos-tab-panel #resource-title').innerText(), /Organization natures|Naturezas das organizações/i);
        await page.getByRole('button', { name: /New record|Novo registro/i }).waitFor();

        for (const [tabName, titlePattern] of [
            [/Economic activities|Atividades econômicas/i, /Economic activities|Atividades econômicas/i],
            [/Organizations|Organizações/i, /Organizations|Organizações/i],
            [/Organizational unit types|Tipos de unidade organizacional/i, /Organizational unit types|Tipos de unidade organizacional/i],
            [/Organizational units|Unidades organizacionais/i, /Organizational units|Unidades organizacionais/i]
        ]) {
            await page.getByRole('tab', { name: tabName }).click();
            await page.waitForTimeout(80);
            assert.match(await page.locator('.comandos-tab-panel #resource-title').innerText(), titlePattern);
            await page.getByRole('button', { name: /New record|Novo registro/i }).waitFor();
        }
        console.log('PASS institutional registrations: every tab is clickable and renders CRUD');

        await page.goto(`${appURL}/erp/core?section=people&resource=person-types`);
        await page.locator('.comandos-tab-panel #resource-title').waitFor();
        assert.match(
            await page.locator('.comandos-tab-panel #resource-title').innerText(),
            /Person types|Tipos de pessoa/i
        );
        await page.getByRole('button', { name: /New record|Novo registro/i }).click();
        await page.getByRole('dialog').waitFor();

        const browserPersonTypeCode = `BROWSER_PERSON_TYPE_${suffix}`;
        await page.locator('#core-code').fill(browserPersonTypeCode);
        await page.locator('#core-name').fill(`Browser person type ${suffix}`);
        await page.locator('#core-description').fill('Browser validation person type');
        await page.getByRole('button', { name: /Save|Salvar/i }).click();
        await page.getByRole('dialog').waitFor({ state: 'detached' });
        await page.getByText(`Browser person type ${suffix}`, { exact: false }).waitFor();
        console.log('PASS person types: create and save through registration form');

        const browserContactTypeName = `Browser contact type ${suffix}`;
        const contactTypeId = await create('core/contact-types', {
            code: `BROWSER_CONTACT_${suffix}`,
            name: browserContactTypeName,
            description: 'Shared contact type validation',
            addressEnabled: true,
            phoneEnabled: true,
            emailEnabled: true,
            active: true
        });

        await page.goto(`${appURL}/erp/core?section=people&resource=contact-types`);
        await page.locator('.comandos-tab-panel #resource-title').waitFor();
        assert.match(
            await page.locator('.comandos-tab-panel #resource-title').innerText(),
            /Contact types|Tipos de contato/i
        );
        await page.getByText(browserContactTypeName, { exact: false }).waitFor();
        assert.equal(
            (await get(`core/contact-types/${contactTypeId}`)).name,
            browserContactTypeName
        );

        await page.getByRole('tab', { name: /People|Pessoas/i }).click();
        await page.getByRole('button', { name: /New record|Novo registro/i }).click();
        await page.getByRole('dialog').waitFor();

        await page.getByRole('button', { name: /Adicionar endereço|Add address/i }).click();
        await page.locator('#person-address-type-0').click();
        await page.getByText(browserContactTypeName, { exact: true }).waitFor();
        await page.keyboard.press('Escape');

        await page.getByRole('button', { name: /Telefones|Phones/i }).click();
        await page.getByRole('button', { name: /Adicionar telefone|Add phone/i }).click();
        await page.locator('#person-phone-type-0').click();
        await page.getByText(browserContactTypeName, { exact: true }).waitFor();
        await page.keyboard.press('Escape');

        await page.getByRole('button', { name: /E-mails/i }).click();
        await page.getByRole('button', { name: /Adicionar e-mail|Add e-mail/i }).click();
        await page.locator('#person-email-type-0').click();
        await page.getByText(browserContactTypeName, { exact: true }).waitFor();
        await page.keyboard.press('Escape');

        await page.getByRole('button', { name: /Cancel|Cancelar/i }).click();
        console.log('PASS contact types: one shared parameter feeds address, phone and e-mail selectors');

        await page.goto(`${appURL}/erp/core?section=institutional&resource=organizations`);
        await page.locator('.comandos-tab-panel #resource-title').waitFor();
        await page.getByRole('button', { name: /New record|Novo registro/i }).waitFor();
        await page.getByRole('tab', { name: /Organizations|Organizações/i }).click();
        await page.getByRole('button', { name: /New record|Novo registro/i }).click();
        await page.getByRole('dialog').waitFor();

        async function assertSelectLabelFloatsFromCenter(triggerId) {
            const trigger = page.locator(`#${triggerId}`);
            const label = page.locator(`label[for="${triggerId}"]`);
            await trigger.waitFor();
            await label.waitFor();

            const beforeTrigger = await trigger.boundingBox();
            const beforeLabel = await label.boundingBox();
            assert.ok(beforeTrigger && beforeLabel, `${triggerId}: trigger/label must be visible`);

            const beforeControlCenter = beforeTrigger.y + beforeTrigger.height / 2;
            const beforeLabelCenter = beforeLabel.y + beforeLabel.height / 2;
            assert.ok(
                Math.abs(beforeControlCenter - beforeLabelCenter) <= 5,
                `${triggerId}: empty select label must start vertically centered`
            );

            await trigger.click();
            await page.waitForTimeout(80);

            const afterTrigger = await trigger.boundingBox();
            const afterLabel = await label.boundingBox();
            assert.ok(afterTrigger && afterLabel, `${triggerId}: trigger/label must remain visible`);
            assert.ok(
                afterLabel.y < beforeLabel.y - 6,
                `${triggerId}: select label must float upward after focus`
            );
            assert.ok(
                afterLabel.y >= afterTrigger.y
                && afterLabel.y + afterLabel.height <= afterTrigger.y + afterTrigger.height,
                `${triggerId}: floated label must stay inside select`
            );

            await page.keyboard.press('Escape');
            await page.locator('body').click({ position: { x: 2, y: 2 } });
        }

        await assertSelectLabelFloatsFromCenter('core-natureId');
        await assertSelectLabelFloatsFromCenter('core-economicActivityId');

        const natureInput = page.locator('#core-natureId');
        await natureInput.click();
        await page.waitForTimeout(100);
        assert.equal(
            await page.getByText('Public safety', { exact: true }).count(),
            0,
            'Nature options must stay hidden before the user types'
        );
        await natureInput.fill('Public');
        await page.getByText('Public safety', { exact: true }).waitFor();
        await natureInput.fill('');
        await page.waitForTimeout(100);
        assert.equal(
            await page.getByText('Public safety', { exact: true }).count(),
            0,
            'Nature options must hide again when the query is cleared'
        );
        console.log('PASS searchable selects: options appear only after typing');

        const natureBox = await page.locator('#core-natureId').boundingBox();
        const activityBox = await page.locator('#core-economicActivityId').boundingBox();
        const nameBox = await page.locator('#core-name').boundingBox();
        assert.ok(natureBox && activityBox && nameBox, 'Organization fields must be visible');
        assert.ok(
            Math.abs(natureBox.height - activityBox.height) <= 1
            && Math.abs(natureBox.height - nameBox.height) <= 1,
            `Organization field heights must match: nature=${natureBox.height}, activity=${activityBox.height}, name=${nameBox.height}`
        );

        console.log('PASS organization select labels and heights: select fields match regular inputs');

        await page.getByRole('button', { name: /Cancel|Cancelar/i }).click();

        await page.goto(`${appURL}/erp/core?section=access&resource=profiles`);
        await page.locator('.comandos-tab-panel #resource-title').waitFor();

        for (const [tabName, titlePattern] of [
            [/Access profile levels|Níveis de perfil de acesso/i, /Access profile levels|Níveis de perfil de acesso/i],
            [/Permission resources|Recursos de permissão/i, /Permission resources|Recursos de permissão/i],
            [/Permission actions|Ações de permissão/i, /Permission actions|Ações de permissão/i]
        ]) {
            await page.getByRole('tab', { name: tabName }).click();
            await page.waitForTimeout(80);
            assert.match(
                await page.locator('.comandos-tab-panel #resource-title').innerText(),
                titlePattern
            );
            await page.getByRole('button', { name: /New record|Novo registro/i }).waitFor();
        }
        console.log('PASS access parameters: parameter tabs are clickable and render CRUD');

        await page.getByRole('tab', { name: /Access profiles|Perfis de acesso/i }).click();
        await page.waitForTimeout(80);
        assert.match(
            await page.locator('.comandos-tab-panel #resource-title').innerText(),
            /Access profiles|Perfis de acesso/i
        );
        await page.getByRole('button', { name: /New record|Novo registro/i }).click();
        await page.getByRole('dialog').waitFor();
        await page.locator('#core-levelTypeId').waitFor();

        const levelHelp = page.getByRole('button', { name: /Ajuda sobre o campo|Field help/i });
        await levelHelp.waitFor();
        const levelHint = page.getByRole('tooltip');
        assert.equal(await levelHint.isVisible(), false, 'Access level hint must not occupy fixed form space');

        await levelHelp.hover();
        await page.waitForTimeout(80);
        assert.equal(await levelHint.isVisible(), true, 'Access level hint must appear on desktop hover');

        await page.mouse.move(1, 1);
        await page.waitForTimeout(80);
        await levelHelp.click();
        assert.equal(await levelHint.isVisible(), true, 'Access level hint must appear on click/tap');
        await levelHelp.click();
        assert.equal(await levelHint.isVisible(), false, 'Access level hint must close on second click');

        console.log('PASS access profiles: parameterized level field and contextual help behavior');
        await page.getByRole('button', { name: /Cancel|Cancelar/i }).click();

        await page.goto(`${appURL}/erp/core?section=access&resource=permissions`);
        await page.getByRole('button', { name: /New record|Novo registro/i }).click();
        await page.getByRole('dialog').waitFor();
        await page.locator('#core-resourceTypeId').waitFor();
        await page.locator('#core-actionTypeId').waitFor();

        for (const fieldId of ['core-resourceTypeId', 'core-actionTypeId']) {
            const fieldset = page.locator(`#${fieldId}`).locator('xpath=ancestor::fieldset[1]');
            const help = fieldset.getByRole('button', { name: /Ajuda sobre o campo|Field help/i });
            const tooltip = fieldset.getByRole('tooltip');
            assert.equal(await tooltip.isVisible(), false, `${fieldId}: tooltip must start hidden`);
            await help.hover();
            await page.waitForTimeout(80);
            assert.equal(await tooltip.isVisible(), true, `${fieldId}: tooltip must appear on desktop hover`);
            await page.mouse.move(1, 1);
        }
        console.log('PASS permissions: resource/action are parameterized and use contextual tooltips');
        await page.getByRole('button', { name: /Cancel|Cancelar/i }).click();

        await page.goto(`${appURL}/erp/core?section=access&resource=user-profiles`);
        await page.getByRole('button', { name: /New record|Novo registro/i }).click();
        await page.getByRole('dialog').waitFor();
        const unitFieldset = page.locator('#core-unitId').locator('xpath=ancestor::fieldset[1]');
        const unitHelp = unitFieldset.getByRole('button', { name: /Ajuda sobre o campo|Field help/i });
        const unitTooltip = unitFieldset.getByRole('tooltip');
        const unitHelpBox = await unitHelp.boundingBox();
        assert.ok(
            unitHelpBox && unitHelpBox.width <= 24 && unitHelpBox.height <= 24,
            'Unit help marker must stay compact beside the select'
        );
        assert.equal(await unitTooltip.isVisible(), false, 'Unit tooltip must start hidden');
        await unitHelp.hover();
        await page.waitForTimeout(80);
        assert.equal(await unitTooltip.isVisible(), true, 'Unit tooltip must appear on desktop hover');
        console.log('PASS user profiles: Unit help uses contextual tooltip');
        await page.getByRole('button', { name: /Cancel|Cancelar/i }).click();

        await page.goto(`${appURL}/erp/core?section=institutional&resource=organizations`);
        await page.getByRole('button', { name: /New record|Novo registro/i }).click();
        await page.getByRole('dialog').waitFor();
        const organizationTaxId = page.locator('#core-taxId');
        await organizationTaxId.fill('12345678000195');
        assert.equal(
            await organizationTaxId.inputValue(),
            '12.345.678/0001-95',
            'Organization CNPJ must be masked while typing'
        );
        console.log('PASS organizations: CNPJ input mask');
        await page.getByRole('button', { name: /Cancel|Cancelar/i }).click();

        await page.goto(`${appURL}/erp/inventory?section=technical-parameters&resource=calibers`);
        await page.locator('.comandos-tab-panel #resource-title').waitFor();
        assert.match(await page.locator('.comandos-tab-panel #resource-title').innerText(), /Calibers|Calibres/i);
        await page.getByRole('button', { name: /New record|Novo registro/i }).waitFor();

        const technicalTabs = page.locator('.comandos-tabs-list');
        await technicalTabs.waitFor();
        const isScrollable = await technicalTabs.evaluate(element => element.scrollWidth > element.clientWidth);
        assert.equal(isScrollable, true, 'Technical parameter tabs must be horizontally scrollable');

        const nextTabs = page.locator('.comandos-tabs-scroll-button-next.is-visible');
        await nextTabs.waitFor();
        assert.equal((await nextTabs.innerText()).trim(), '>');
        assert.equal(await nextTabs.evaluate(element => getComputedStyle(element).backgroundColor), 'rgba(0, 0, 0, 0)');

        await page.getByRole('tab', { name: /Ammunition types|Tipos de munição/i }).click();
        assert.match(await page.locator('.comandos-tab-panel #resource-title').innerText(), /Ammunition types|Tipos de munição/i);

        for (let index = 0; index < 8; index += 1) {
            if (await page.getByRole('tab', { name: /Interfaces/i }).isVisible()) break;
            await nextTabs.click();
            await page.waitForTimeout(120);
        }
        await page.getByRole('tab', { name: /Interfaces/i }).click();
        assert.match(await page.locator('.comandos-tab-panel #resource-title').innerText(), /Interfaces/i);
        await page.getByRole('button', { name: /New record|Novo registro/i }).waitFor();
        console.log('PASS technical registrations: tabs clickable, scrollable and arrows are transparent symbols');

        await page.goto(`${appURL}/erp/transactions`);
        const acquisitionType = page.locator('#acquisition-type');
        await acquisitionType.waitFor();
        assert.equal(
            await acquisitionType.inputValue(),
            'Onerosa / compra',
            'Fixed selects must render the translated option label instead of the internal value'
        );
        await acquisitionType.click();
        await page.getByText('Onerosa / compra', { exact: true }).waitFor();
        await page.getByText('Gratuita', { exact: true }).waitFor();
        await page.keyboard.press('Escape');

        await page.getByRole('tab', { name: /Custody|Cautela/i }).click();
        const recipientType = page.locator('#custody-recipient-type');
        await recipientType.waitFor();
        assert.equal(
            await recipientType.inputValue(),
            'Pessoa',
            'Custody recipient type must render Pessoa instead of PERSON'
        );
        await recipientType.click();
        await page.getByText('Pessoa', { exact: true }).waitFor();
        await page.getByText(/Organizational unit|Unidade organizacional/i, { exact: true }).waitFor();
        await page.keyboard.press('Escape');
        console.log('PASS transactions: fixed selects render localized labels and open without typing');

        async function assertReferenceDataLayout(resource, horizontalFlags) {
            await page.goto(`${appURL}/erp/inventory?section=reference-data&resource=${resource}`);
            await page.getByRole('button', { name: /New record|Novo registro/i }).click();
            const dialog = page.getByRole('dialog');
            await dialog.waitFor();

            const description = dialog.locator('[data-comandos-field-name="description"]');
            const displayOrder = dialog.locator('[data-comandos-field-name="displayOrder"]');
            const descriptionBox = await description.boundingBox();
            const displayOrderBox = await displayOrder.boundingBox();
            assert.ok(descriptionBox && displayOrderBox, `${resource}: description/displayOrder fields must be visible`);
            assert.ok(
                Math.abs(descriptionBox.y - displayOrderBox.y) <= 3 && displayOrderBox.x > descriptionBox.x,
                `${resource}: display order must occupy the former Active position beside Description`
            );

            const flagBoxes = [];
            for (const fieldName of horizontalFlags) {
                const box = await dialog.locator(`[data-comandos-field-name="${fieldName}"]`).boundingBox();
                assert.ok(box, `${resource}: ${fieldName} must be visible`);
                flagBoxes.push(box);
            }
            const flagTop = flagBoxes[0].y;
            assert.ok(
                flagBoxes.every(box => Math.abs(box.y - flagTop) <= 3),
                `${resource}: boolean flags must stay on the same horizontal row`
            );
            for (let index = 1; index < flagBoxes.length; index += 1) {
                assert.ok(
                    flagBoxes[index].x > flagBoxes[index - 1].x,
                    `${resource}: boolean flags must keep left-to-right order`
                );
            }

            await page.getByRole('button', { name: /Cancel|Cancelar/i }).click();
        }

        await assertReferenceDataLayout(
            'custody-return-condition-types',
            ['active', 'blocksAvailability', 'systemProtected']
        );
        await assertReferenceDataLayout(
            'sale-return-reason-types',
            ['active', 'systemProtected']
        );
        await assertReferenceDataLayout(
            'inventory-count-status-types',
            ['active', 'terminal', 'systemProtected']
        );
        console.log('PASS reference data forms: display order and boolean flag alignment');

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
