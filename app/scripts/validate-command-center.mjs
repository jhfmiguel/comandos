import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { spawn } from 'node:child_process';
import { mkdtemp, mkdir, readFile, writeFile } from 'node:fs/promises';
import { resolve, join } from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';
const { chromium } = createRequire(resolve('../wr-api/target/browser-validation/package.json'))('playwright');
const base = await mkdtemp(resolve('../wr-api/target/command-center-'));
const automation = join(base, 'automation');
await mkdir(join(automation, 'runtime'), { recursive: true });
const fakeCommand = join(base, 'fake.cmd');
const worker = (await readFile('../automation/codex-erp-bot.ps1', 'utf8')).replace("[string]$CodexCommand = 'codex'", `[string]$CodexCommand = '${fakeCommand.replaceAll("'", "''")}'`);
await writeFile(join(automation, 'codex-erp-bot.ps1'), worker);
await writeFile(join(base, 'fake.cjs'), "process.stdin.resume(); process.stdin.on('end',()=>setTimeout(()=>console.log('validated'),4000));");
await writeFile(fakeCommand, `@echo off\r\n"${process.execPath}" "${join(base, 'fake.cjs')}"\r\n`);
const statusPath = join(automation, 'runtime/status.json');
await writeFile(statusPath, '\uFEFF' + JSON.stringify({ state: 'working', message: 'Status antigo', taskName: '', processedTasks: 0, updatedAt: '2020-01-01T00:00:00Z', pid: process.pid }));
const server = spawn(process.execPath, ['node_modules/next/dist/bin/next', 'start', '--port', '3100'], { env: { ...process.env, COMANDOS_AUTOMATION_ROOT: automation }, windowsHide: true });
let serverLog = '';
server.stdout.on('data', data => serverLog += data); server.stderr.on('data', data => serverLog += data);
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const context = await browser.newContext();
const page = await context.newPage();
page.setDefaultTimeout(20000);
const errors = [];
page.on('pageerror', error => errors.push(error.message));
const url = 'http://localhost:3100';
async function until(check) {
    for (let attempt = 0; attempt < 150; attempt++) { try { if (await check()) return; } catch {} await delay(200); }
    throw new Error('Timed out: ' + serverLog);
}
const post = (path, data) => context.request.post(url + path, { data });
try {
    await until(async () => (await context.request.get(url + '/api/bot/status')).ok());
    let status = await (await context.request.get(url + '/api/bot/status')).json();
    assert.equal(status.state, 'unresponsive');
    assert.equal(status.message.includes('90 segundos'), true);
    await writeFile(statusPath, JSON.stringify({ state: 'working', pid: 2147483647, updatedAt: new Date().toISOString() }));
    assert.equal((await (await context.request.get(url + '/api/bot/status')).json()).state, 'offline');
    assert.equal((await post('/api/bot/control', { command: 'resume' })).status(), 409);
    assert.equal((await post('/api/bot/control', { command: 'replace' })).status(), 400);
    assert.equal((await post('/api/bot/tasks', { id: '../escape', module: 'armamento', markdown: 'bad' })).status(), 400);
    assert.equal((await context.request.post(url + '/api/bot/control', { headers: { origin: 'http://other.example' }, data: { command: 'start', workstream: 'armamento' } })).status(), 403);
    console.log('PASS BOM, stale/dead worker, offline control, invalid task and cross-origin rejection');

    await page.addInitScript(() => { if (!localStorage.getItem('test-initialized')) { localStorage.setItem('comandos-requirements', '{invalid'); localStorage.setItem('test-initialized', 'true'); } });
    await page.goto(url + '/bot');
    await page.getByText('Não foi possível carregar o backlog salvo no navegador.', { exact: false }).waitFor();
    await page.getByRole('button', { name: 'Novo item', exact: true }).click();
    await page.getByLabel('Título', { exact: true }).fill('Validação do Command Center');
    await page.getByLabel('Descrição', { exact: true }).fill('Tarefa isolada para validar a fila e o worker simulado.');
    await page.getByLabel('Critérios de aceite', { exact: true }).fill('Concluir sem alterar o projeto.');
    await page.getByRole('button', { name: 'Adicionar item', exact: true }).click();
    const sent = page.waitForResponse(response => response.url().endsWith('/api/bot/tasks'));
    await page.getByRole('button', { name: 'Enviar ao bot', exact: true }).click();
    const receipt = await (await sent).json();
    assert.ok(receipt.taskName);
    const markdown = await readFile(join(automation, 'tasks', receipt.taskName), 'utf8');
    assert.match(markdown, /Validação do Command Center/);
    await page.getByRole('button', { name: 'Enviar ao bot', exact: true }).click();
    await page.getByText('Esta tarefa já está registrada na fila.', { exact: true }).waitFor();
    status = await (await context.request.get(url + '/api/bot/status')).json();
    assert.equal(status.tasks.length, 1);
    await page.reload();
    await page.getByRole('heading', { name: 'Validação do Command Center', exact: true }).waitFor();
    console.log('PASS corrupt local storage recovery, task enqueue, retry without duplication and reload');

    const started = page.waitForResponse(response => response.url().endsWith('/api/bot/control'));
    await page.getByRole('button', { name: 'Iniciar próxima etapa', exact: true }).click();
    const startResponse = await started;
    assert.equal(startResponse.status(), 202, await startResponse.text());
    await until(async () => (await (await context.request.get(url + '/api/bot/status')).json()).state === 'working');
    assert.equal((await post('/api/bot/control', { command: 'start', workstream: 'armamento' })).status(), 409);
    await page.getByRole('button', { name: 'Pausar após etapa', exact: true }).click();
    await until(async () => (await (await context.request.get(url + '/api/bot/status')).json()).state === 'paused');
    await page.getByRole('button', { name: 'Continuar', exact: true }).click();
    await until(async () => {
        const result = await (await context.request.get(url + '/api/bot/status')).json();
        return result.state === 'completed' && !result.alive;
    });
    status = await (await context.request.get(url + '/api/bot/status')).json();
    assert.equal(status.tasks[0].state, 'completed');
    assert.equal(status.processedTasks, 1);
    console.log('PASS real control API starts isolated worker, pause/resume and completion confirmed');
    assert.deepEqual(errors, []);
    console.log(`Evidence: ${base}`);
} finally {
    await browser.close();
    try { const status = JSON.parse(await readFile(statusPath, 'utf8')); if (status.pid !== process.pid && status.pid !== 2147483647) await writeFile(join(automation, 'runtime/control.json'), '{"command":"stop"}'); } catch {}
    await new Promise(resolve => { const stop = spawn('taskkill.exe', ['/PID', String(server.pid), '/T', '/F'], { windowsHide: true, stdio: 'ignore' }); stop.on('exit', resolve); });
}
