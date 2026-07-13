// Collaboration layout stitcher.
// bpmn-auto-layout only lays out single processes. This wraps that: each participant's
// process is laid out independently, the results are stacked as vertical pools, each pool
// gets a participant shape, and message flows are drawn as orthogonal edges between the
// referenced flow nodes across pools. Produces one BPMNDiagram on the collaboration.

const POOL_LABEL_W = 30;   // left header strip of a pool
const PAD = 30;            // padding inside a pool around its flow nodes
const POOL_GAP = 60;       // vertical gap between pools

function attr(xml, name) {
  const m = xml.match(new RegExp(name + '="([^"]*)"'));
  return m ? m[1] : null;
}

// pull <bpmn:process ...>...</bpmn:process> blocks (any prefix)
function extractProcesses(xml) {
  const out = [];
  const re = /<(\w+:)?process\b[\s\S]*?<\/(\w+:)?process>/g;
  let m;
  while ((m = re.exec(xml))) {
    const block = m[0];
    out.push({ id: attr(block, 'id'), xml: block });
  }
  return out;
}

function extractParticipants(xml) {
  const out = [];
  const re = /<(\w+:)?participant\b[^>]*\/?>/g;
  let m;
  while ((m = re.exec(xml))) {
    out.push({ id: attr(m[0], 'id'), name: attr(m[0], 'name') || '', processRef: attr(m[0], 'processRef') });
  }
  return out;
}

function extractMessageFlows(xml) {
  const out = [];
  const re = /<(\w+:)?messageFlow\b[^>]*\/?>/g;
  let m;
  while ((m = re.exec(xml))) {
    out.push({ id: attr(m[0], 'id'), sourceRef: attr(m[0], 'sourceRef'), targetRef: attr(m[0], 'targetRef') });
  }
  return out;
}

// offset every x=/y= coordinate in a DI chunk (widths/heights are unaffected — different attrs)
function offset(chunk, dx, dy) {
  return chunk
    .replace(/\bx="(-?\d+(?:\.\d+)?)"/g, (_, n) => `x="${(+n + dx).toFixed(2)}"`)
    .replace(/\by="(-?\d+(?:\.\d+)?)"/g, (_, n) => `y="${(+n + dy).toFixed(2)}"`);
}

export async function stitchCollaboration(semanticXml, layoutProcess) {
  const collabId = (semanticXml.match(/<(?:\w+:)?collaboration\b[^>]*id="([^"]+)"/) || [])[1] || 'Collaboration';
  const participants = extractParticipants(semanticXml);
  const messageFlows = extractMessageFlows(semanticXml);
  const processes = extractProcesses(semanticXml);
  const procById = Object.fromEntries(processes.map(p => [p.id, p]));

  const poolShapes = [];
  const nodeShapes = [];
  const edgeChunks = [];
  const bounds = {}; // elementId -> {x,y,w,h}
  let currentY = 0;

  for (const part of participants) {
    const proc = procById[part.processRef];
    if (!proc) continue;
    // lay out this process in isolation
    const wrapped = `<?xml version="1.0" encoding="UTF-8"?>\n<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:zeebe="http://camunda.org/schema/zeebe/1.0" id="d_${proc.id}" targetNamespace="http://probuild">${proc.xml}</bpmn:definitions>`;
    const laid = await layoutProcess(wrapped);

    // collect this process's shapes and edges
    const shapes = [...laid.matchAll(/<bpmndi:BPMNShape\b[\s\S]*?<\/bpmndi:BPMNShape>/g)].map(m => m[0]);
    const edges = [...laid.matchAll(/<bpmndi:BPMNEdge\b[\s\S]*?<\/bpmndi:BPMNEdge>/g)].map(m => m[0]);

    // bounding box of raw shapes
    let minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
    for (const s of shapes) {
      const b = s.match(/<dc:Bounds x="(-?\d+(?:\.\d+)?)" y="(-?\d+(?:\.\d+)?)" width="(\d+(?:\.\d+)?)" height="(\d+(?:\.\d+)?)"/);
      if (!b) continue;
      const x = +b[1], y = +b[2], w = +b[3], h = +b[4];
      minX = Math.min(minX, x); minY = Math.min(minY, y);
      maxX = Math.max(maxX, x + w); maxY = Math.max(maxY, y + h);
    }
    const dx = POOL_LABEL_W + PAD - minX;
    const dy = currentY + PAD - minY;
    const poolW = (maxX - minX) + POOL_LABEL_W + 2 * PAD;
    const poolH = (maxY - minY) + 2 * PAD;

    // pool shape (participant)
    poolShapes.push(
      `      <bpmndi:BPMNShape id="${part.id}_di" bpmnElement="${part.id}" isHorizontal="true">\n` +
      `        <dc:Bounds x="0" y="${currentY.toFixed(2)}" width="${poolW.toFixed(2)}" height="${poolH.toFixed(2)}" />\n` +
      `      </bpmndi:BPMNShape>`);

    // offset + record node bounds
    for (const s of shapes) {
      const off = offset(s, dx, dy);
      nodeShapes.push('      ' + off.trim());
      const id = attr(s, 'bpmnElement');
      const b = off.match(/<dc:Bounds x="(-?\d+(?:\.\d+)?)" y="(-?\d+(?:\.\d+)?)" width="(\d+(?:\.\d+)?)" height="(\d+(?:\.\d+)?)"/);
      if (b) bounds[id] = { x: +b[1], y: +b[2], w: +b[3], h: +b[4] };
    }
    for (const e of edges) edgeChunks.push('      ' + offset(e, dx, dy).trim());

    currentY += poolH + POOL_GAP;
  }

  // message-flow edges (orthogonal: source bottom -> down -> across -> target top)
  const mfEdges = messageFlows.map(mf => {
    const s = bounds[mf.sourceRef], t = bounds[mf.targetRef];
    if (!s || !t) return '';
    const sx = s.x + s.w / 2, sy = s.y + s.h;
    const tx = t.x + t.w / 2, ty = t.y;
    const midY = (sy + ty) / 2;
    const wp = [[sx, sy], [sx, midY], [tx, midY], [tx, ty]]
      .map(([x, y]) => `        <di:waypoint x="${x.toFixed(2)}" y="${y.toFixed(2)}" />`).join('\n');
    return `      <bpmndi:BPMNEdge id="${mf.id}_di" bpmnElement="${mf.id}">\n${wp}\n      </bpmndi:BPMNEdge>`;
  }).filter(Boolean);

  const di =
    `  <bpmndi:BPMNDiagram id="BPMNDiagram_${collabId}">\n` +
    `    <bpmndi:BPMNPlane id="BPMNPlane_${collabId}" bpmnElement="${collabId}">\n` +
    [...poolShapes, ...nodeShapes, ...edgeChunks, ...mfEdges].join('\n') + '\n' +
    `    </bpmndi:BPMNPlane>\n` +
    `  </bpmndi:BPMNDiagram>`;

  // inject DI namespaces on the root definitions if missing, and insert DI before </definitions>
  let out = semanticXml;
  const nsAdds = {
    'xmlns:bpmndi': 'http://www.omg.org/spec/BPMN/20100524/DI',
    'xmlns:dc': 'http://www.omg.org/spec/DD/20100524/DC',
    'xmlns:di': 'http://www.omg.org/spec/DD/20100524/DI',
  };
  out = out.replace(/<(\w+:)?definitions\b/, (m) => {
    let add = '';
    for (const [k, v] of Object.entries(nsAdds)) if (!out.includes(k + '=')) add += ` ${k}="${v}"`;
    return m + add;
  });
  out = out.replace(/<\/(\w+:)?definitions>/, `${di}\n</$1definitions>`);
  return out;
}
