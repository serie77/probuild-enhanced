// Build step: read hand-authored *semantic* BPMN from bpmn-src/, generate diagram
// interchange (DI) with bpmn-auto-layout, and emit executable+renderable BPMN into
// src/main/resources/processes/. Single-process files are laid out directly; multi-pool
// collaborations are laid out per-process then stitched into stacked pools with message
// flows by stitch-collab.mjs.
import { layoutProcess } from 'bpmn-auto-layout';
import { stitchCollaboration } from './stitch-collab.mjs';
import fs from 'node:fs';
import path from 'node:path';

const SRC = path.resolve('bpmn-src');
const OUT = path.resolve('src/main/resources/processes');
fs.mkdirSync(OUT, { recursive: true });

const files = fs.readdirSync(SRC).filter(f => f.endsWith('.bpmn'));
let ok = 0;
for (const f of files) {
  const xml = fs.readFileSync(path.join(SRC, f), 'utf8');
  try {
    let out;
    if (/<bpmn:collaboration|<collaboration/.test(xml)) {
      out = await stitchCollaboration(xml, layoutProcess);
    } else {
      out = await layoutProcess(xml);
    }
    fs.writeFileSync(path.join(OUT, f), out);
    const shapes = (out.match(/BPMNShape/g) || []).length;
    console.log(`  ✓ ${f}  (${shapes} shapes)`);
    ok++;
  } catch (e) {
    console.error(`  ✗ ${f}: ${e.message}`);
  }
}
console.log(`layout: ${ok}/${files.length}`);
