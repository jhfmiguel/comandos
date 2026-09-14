import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdtemp, mkdir, readFile, writeFile, copyFile, access } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';

// Runs the real worker against an isolated queue and fake CLI, never the live backlog.
const root = resolve('wr-api/target');
await mkdir(root, { recursive: true });
const directory = await mkdtemp(join(root, 'bot-validation-'));
const markdown = '# Test\n\n## Objetivo\nTest worker\n\n## Escopo\nFixture\n\n## Critérios de aceite\n- [ ] Pass\n\n## Condição de parada\nStop.\n';
async function fixture(name, code) {
    const base = join(directory, name);
    const automation = join(base, 'automation');
    await mkdir(join(automation, 'tasks'), { recursive: true });
    await copyFile('automation/codex-erp-bot.ps1', join(automation, 'codex-erp-bot.ps1'));
    await writeFile(join(automation, 'tasks', 'armamento-001-test.md'), markdown);
    await writeFile(join(base, 'fake.cjs'), code);
    await writeFile(join(base, 'fake.cmd'), `@echo off\r\n"${process.execPath}" "${join(base, 'fake.cjs')}"\r\n`);
    const run = (...args) => {
        const child = spawn('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', join(automation, 'codex-erp-bot.ps1'), '-CodexCommand', join(base, 'fake.cmd'), '-Workstream', 'armamento', '-NoNotification', '-PollSeconds', '2', ...args], { windowsHide: true });
        let output = '';
        child.stdout.on('data', data => output += data); child.stderr.on('data', data => output += data);
        const done = new Promise((res, rej) => { child.on('error', rej); child.on('exit', code => res({ code, output })); });
        return { child, done };
    };
    const status = async () => JSON.parse((await readFile(join(automation, 'runtime/status.json'), 'utf8')).replace(/^\uFEFF/, ''));
    const control = command => writeFile(join(automation, 'runtime/control.json'), JSON.stringify({ command }));
    return { base, automation, run, status, control };
}
async function until(action, timeout = 20000) {
    const end = Date.now() + timeout;
    while (Date.now() < end) { try { if (await action()) return; } catch {} await delay(150); }
    throw new Error('Timed out waiting for worker');
}
async function finish(run) {
    const timer = setTimeout(() => { spawn('taskkill.exe', ['/PID', String(run.child.pid), '/T', '/F'], { windowsHide: true }); }, 25000);
    try { const result = await run.done; assert.equal(result.code, 0, result.output); } finally { clearTimeout(timer); }
}

const flood = await fixture('flood', "process.stdin.resume(); process.stdin.on('end',()=>{ process.stdout.write('x'.repeat(2_000_000)); process.stderr.write('y'.repeat(2_000_000)); });");
await finish(flood.run('-Once'));
assert.equal((await flood.status()).state, 'completed');
await access(join(flood.automation, 'tasks/completed/armamento-001-test.md'));
console.log('PASS large stdout/stderr completes without pipe deadlock');

const rate = await fixture('rate', "console.error('usage limit'); process.exitCode=1;");
await finish(rate.run('-Once'));
assert.equal((await rate.status()).state, 'waiting');
await access(join(rate.automation, 'tasks/armamento-001-test.md'));
console.log('PASS usage limit preserves pending task and Once exits');

const failed = await fixture('failed', "console.error('fixture failure'); process.exitCode=1;");
await finish(failed.run('-Once'));
assert.equal((await failed.status()).state, 'failed');
await access(join(failed.automation, 'tasks/failed/armamento-001-test.md'));
console.log('PASS failed CLI reports failed state');

const paused = await fixture('paused', "console.log('done');");
const pausedRun = paused.run('-StartPaused', '-Once');
await until(async () => (await paused.status()).state === 'paused');
const duplicate = await paused.run('-Once').done;
assert.notEqual(duplicate.code, 0);
await paused.control('resume');
await finish(pausedRun);
assert.equal((await paused.status()).state, 'completed');
console.log('PASS pause/resume and exclusive worker lock');

const stopped = await fixture('stopped', "require('fs').writeFileSync('child.pid',String(process.pid)); setInterval(()=>console.log('alive'),100);");
const stopRun = stopped.run('-Once');
await until(async () => (await stopped.status()).state === 'working');
await until(async () => { await access(join(stopped.base, 'child.pid')); return true; });
const before = (await stopped.status()).updatedAt;
await until(async () => (await stopped.status()).updatedAt !== before);
await stopped.control('stop');
await finish(stopRun);
assert.equal((await stopped.status()).state, 'stopped');
await access(join(stopped.automation, 'tasks/armamento-001-test.md'));
const childPid = Number(await readFile(join(stopped.base, 'child.pid'), 'utf8'));
assert.throws(() => process.kill(childPid, 0));
console.log('PASS heartbeat, stop kills descendants and requeues task');

await mkdir(join(paused.automation, 'tasks/working'), { recursive: true });
await writeFile(join(paused.automation, 'tasks/working/armamento-002-orphan.md'), markdown);
await finish(paused.run('-Once'));
await access(join(paused.automation, 'tasks/completed/armamento-002-orphan.md'));
console.log('PASS abandoned task recovered on next start');
console.log(`Evidence: ${directory}`);
