async function api(method, url, body) {
  const res = await fetch(url, {
    method: method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined
  });
  let data = null;
  try { data = await res.json(); } catch (e) { /* empty body */ }
  return { status: res.status, data: data };
}
function qs(name) { return new URLSearchParams(window.location.search).get(name); }
function setMsg(text, ok) {
  const el = document.getElementById('msg');
  el.textContent = text;
  el.className = 'msg ' + (ok ? 'ok' : 'err');
}
function sleep(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }
