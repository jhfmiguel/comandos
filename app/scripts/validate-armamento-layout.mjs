import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';

const output = new URL('../../api/target/browser-validation/armamento/', import.meta.url);
await mkdir(output, { recursive: true });

const frontend = process.env.COMANDOS_APP_URL || 'http://localhost:3000';
const backend = process.env.COMANDOS_API_URL || 'http://localhost:8180';

const routes = [
  'inventory',
  'receivings',
  'receiving-inspections',
  'receiving-incorporations',
  'custody',
  'transfers',
  'maintenance',
  'reservations',
  'ammunition-consumption',
  'donations',
  'sales',
  'disposals',
  'inventory-counts',
  'lifecycle',
  'governance',
  'audit'
];

const browser = await chromium.launch({ headless: true });
const results = [];

try {
  const apiContext = await browser.newContext();
  const health = await apiContext.request.get(`${backend}/actuator/health`);
  assert.equal(health.ok(), true, `Backend health failed: ${health.status()}`);
  await apiContext.close();

  for (const width of [390, 768, 1440]) {
    for (const route of routes) {
      const context = await browser.newContext({ viewport: { width, height: 900 } });
      const page = await context.newPage();
      page.setDefaultTimeout(20000);
      const errors = [];

      page.on('pageerror', error => errors.push(`JavaScript: ${error.message}`));
      page.on('console', message => {
        if (message.type() === 'error') errors.push(`Console: ${message.text()}`);
      });
      page.on('response', response => {
        if (response.status() >= 500) errors.push(`HTTP ${response.status()}: ${response.url()}`);
      });
      page.on('requestfailed', request => {
        if (request.failure()?.errorText !== 'net::ERR_ABORTED') {
          errors.push(`Network: ${request.url()} ${request.failure()?.errorText}`);
        }
      });

      let dimensions;
      try {
        const response = await page.goto(`${frontend}/erp/${route}`);
        assert.equal(response?.status(), 200);
        await page.waitForLoadState('networkidle');
        assert.ok(await page.locator('main, .registration-page').first().innerText(), 'Main content must render');
        dimensions = await page.evaluate(() => ({
          viewport: document.documentElement.clientWidth,
          content: document.documentElement.scrollWidth,
        }));
        assert.ok(dimensions.content <= dimensions.viewport + 1,
          `Horizontal overflow: ${JSON.stringify(dimensions)}`);
      } catch (error) {
        errors.push(error.message);
      } finally {
        await page.screenshot({
          path: fileURLToPath(new URL(`${route}-${width}.png`, output)),
          fullPage: true
        }).catch(error => errors.push(`Screenshot: ${error.message}`));

        results.push({
          route,
          width,
          dimensions,
          errors,
          result: errors.length ? 'FAIL' : 'PASS'
        });
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
