import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';

// Requires the production frontend on 3100 and the isolated PostgreSQL API on 8180.
// This read-only smoke check supplements, but does not replace, the workflow scripts.
const output = new URL('../../wr-api/target/browser-validation/armamento-022/', import.meta.url);
await mkdir(output, { recursive: true });
const routes = ['inventory', 'receivings', 'custody', 'transfers', 'maintenance', 'disposals', 'inventory-counts'];
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const results = [];
try {
    for (const width of [390, 768, 1440]) {
        for (const route of routes) {
            const context = await browser.newContext({ viewport: { width, height: 900 } });
            const page = await context.newPage();
            page.setDefaultTimeout(15000);
            const errors = [];
            let apiResponses = 0;
            page.on('pageerror', error => errors.push(`JavaScript: ${error.message}`));
            page.on('console', message => {
                if (message.type() === 'error') errors.push(`Console: ${message.text()}`);
            });
            page.on('response', response => {
                if (response.url().includes('/api/erp/')) apiResponses++;
                if (response.status() >= 400) errors.push(`HTTP ${response.status()}: ${response.url()}`);
            });
            page.on('requestfailed', request => {
                if (request.failure()?.errorText !== 'net::ERR_ABORTED') {
                    errors.push(`Network: ${request.url()} ${request.failure()?.errorText}`);
                }
            });
            await context.route('http://localhost:8080/api/**', route => route.continue({
                url: route.request().url().replace('localhost:8080', 'localhost:8180'),
            }));
            let dimensions;
            try {
                const response = await page.goto(`http://localhost:3100/erp/${route}`);
                assert.equal(response.status(), 200);
                await page.waitForLoadState('networkidle');
                assert.ok(await page.locator('main').innerText(), 'Main content must render');
                assert.ok(apiResponses > 0, 'Must exercise the real API');
                dimensions = await page.evaluate(() => ({
                    viewport: document.documentElement.clientWidth,
                    content: document.documentElement.scrollWidth,
                }));
                assert.ok(dimensions.content <= dimensions.viewport + 1,
                    `Horizontal overflow: ${JSON.stringify(dimensions)}`);
            } catch (error) {
                errors.push(error.message);
            } finally {
                await page.screenshot({ path: fileURLToPath(new URL(`${route}-${width}.png`, output)), fullPage: true })
                    .catch(error => errors.push(`Screenshot: ${error.message}`));
                results.push({ route, width, dimensions, apiResponses, errors, result: errors.length ? 'FAIL' : 'PASS' });
                console.log(JSON.stringify(results.at(-1)));
                await context.close();
            }
        }
    }
} finally {
    await browser.close();
    await writeFile(new URL('results.json', output), JSON.stringify(results, null, 2));
}
if (results.some(result => result.result === 'FAIL')) process.exitCode = 1;
