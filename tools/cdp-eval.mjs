// Evalúa una expresión JS en el WebView vía Chrome DevTools Protocol.
// Uso: node cdp-eval.mjs "<expresión>"
const expr = process.argv[2];
const list = await fetch('http://127.0.0.1:9222/json').then(r => r.json());
const page = list.find(p => p.type === 'page');
if (!page) { console.error('No hay página'); process.exit(1); }
const ws = new WebSocket(page.webSocketDebuggerUrl);
const result = await new Promise((resolve, reject) => {
  ws.onopen = () => ws.send(JSON.stringify({
    id: 1,
    method: 'Runtime.evaluate',
    params: { expression: expr, returnByValue: true }
  }));
  ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id === 1) resolve(msg.result);
  };
  ws.onerror = reject;
  setTimeout(() => reject(new Error('timeout')), 8000);
});
console.log(JSON.stringify(result, null, 2));
ws.close();
