/* Icon maker: photo -> square 72x72 launcher icon in the style of Sony's PlayMemories icons.
 * Canvas port of plans/icon-maker/make_app_icon.py (Pillow), "square" shape.
 *
 * The camera shows the file as stored. The 66x66 card sits at (3,2) in the 72x72 file, leaving
 * room below it for the card shadow. The photo crop is square and the badge keeps its aspect ratio.
 */
(() => {
  'use strict';

  // ---- geometry (matches make_app_icon.py, "card" shape) -------------------------------------
  const FILE_W = 72, FILE_H = 72;
  const CARD_X = 3, CARD_Y = 2, CARD_W = 66, CARD_H = 66;
  const BADGE_SVG_W = 60, BADGE_SVG_H = 32;
  const CARD_ASPECT = CARD_W / CARD_H; // crop window aspect (1:1)
  const CARD_SHADOW_DY = 2;
  const CARD_SHADOW_BLUR = 1.3;
  const BADGE_SHADOW_DX = 1, BADGE_SHADOW_DY = 2, BADGE_SHADOW_BLUR = 1.1;
  const SHARPEN_SIGMA = 0.8;
  const SHARPEN_THRESHOLD = 1;
  const BADGE_SS = 8; // badge is rendered this much larger, then stepped down

  // ---- parameters ------------------------------------------------------------------------------
  const PARAMS = [
    { id: 'panX', label: 'Pan X', min: 0, max: 1, step: 0.005, def: 0.55, dp: 3 },
    { id: 'panY', label: 'Pan Y', min: 0, max: 1, step: 0.005, def: 0.5, dp: 3 },
    { id: 'zoom', label: 'Zoom', min: 1, max: 4, step: 0.01, def: 1, dp: 2 },
    { id: 'badgeW', label: 'Badge width', min: 30, max: 64, step: 1, def: 48, dp: 0, group: true },
    { id: 'badgeTop', label: 'Badge top', min: 0, max: 50, step: 1, def: 7, dp: 0 },
    { id: 'saturation', label: 'Saturation', min: 0, max: 2, step: 0.01, def: 1.15, dp: 2, group: true },
    { id: 'contrast', label: 'Contrast', min: 0, max: 2, step: 0.01, def: 1.08, dp: 2 },
    { id: 'sharpen', label: 'Sharpen', min: 0, max: 2, step: 0.01, def: 0.6, dp: 2 },
    { id: 'border', label: 'Border bright', min: 0, max: 1, step: 0.01, def: 0.68, dp: 2, group: true },
    { id: 'radius', label: 'Corner radius', min: 0, max: 10, step: 0.5, def: 4, dp: 1 },
    { id: 'badgeShadow', label: 'Badge shadow', min: 0, max: 1, step: 0.01, def: 0.75, dp: 2 },
    { id: 'cardShadow', label: 'Card shadow', min: 0, max: 1, step: 0.01, def: 0.55, dp: 2 },
  ];
  const STORE_KEY = 'iconmaker.params.v2';

  const params = {};
  for (const p of PARAMS) params[p.id] = p.def;

  function loadParams() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORE_KEY) || '{}');
      for (const p of PARAMS) {
        const v = saved[p.id];
        if (typeof v === 'number' && isFinite(v)) params[p.id] = Math.min(p.max, Math.max(p.min, v));
      }
    } catch (e) { /* storage unavailable or corrupt: use defaults */ }
  }
  function saveParams() {
    try { localStorage.setItem(STORE_KEY, JSON.stringify(params)); } catch (e) { /* ignore */ }
  }

  // ---- small helpers -----------------------------------------------------------------------------
  const $ = (id) => document.getElementById(id);
  const clamp255 = (v) => (v < 0 ? 0 : v > 255 ? 255 : v);

  function makeCanvas(w, h) {
    const c = document.createElement('canvas');
    c.width = w; c.height = h;
    return c;
  }
  function ctx2d(c, opts) {
    const g = c.getContext('2d', opts);
    g.imageSmoothingEnabled = true;
    g.imageSmoothingQuality = 'high';
    return g;
  }

  // Resample `src` (canvas/image) to w x h: step down by halves, then a final high-quality draw.
  function resample(src, sx, sy, sw, sh, w, h) {
    let cur = src, cx = sx, cy = sy, cw = sw, ch = sh;
    for (;;) {
      const nw = cw / 2 >= w ? Math.ceil(cw / 2) : cw;
      const nh = ch / 2 >= h ? Math.ceil(ch / 2) : ch;
      if (nw === cw && nh === ch) break;
      const c = makeCanvas(nw, nh);
      ctx2d(c).drawImage(cur, cx, cy, cw, ch, 0, 0, nw, nh);
      cur = c; cx = 0; cy = 0; cw = nw; ch = nh;
    }
    const out = makeCanvas(w, h);
    ctx2d(out).drawImage(cur, cx, cy, cw, ch, 0, 0, w, h);
    return out;
  }

  // Separable Gaussian blur of one plane (Float32Array w*h), edges clamped.
  function gaussKernel(sigma) {
    const r = Math.max(1, Math.ceil(sigma * 3));
    const k = new Float32Array(2 * r + 1);
    let sum = 0;
    for (let i = -r; i <= r; i++) { k[i + r] = Math.exp(-(i * i) / (2 * sigma * sigma)); sum += k[i + r]; }
    for (let i = 0; i < k.length; i++) k[i] /= sum;
    return k;
  }
  function blurPlane(src, w, h, sigma) {
    const k = gaussKernel(sigma), r = (k.length - 1) >> 1;
    const tmp = new Float32Array(w * h), out = new Float32Array(w * h);
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        let s = 0;
        for (let i = -r; i <= r; i++) {
          const xx = Math.min(w - 1, Math.max(0, x + i));
          s += src[y * w + xx] * k[i + r];
        }
        tmp[y * w + x] = s;
      }
    }
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        let s = 0;
        for (let i = -r; i <= r; i++) {
          const yy = Math.min(h - 1, Math.max(0, y + i));
          s += tmp[yy * w + x] * k[i + r];
        }
        out[y * w + x] = s;
      }
    }
    return out;
  }

  // ---- source image ------------------------------------------------------------------------------
  let source = null;          // { canvas, w, h, name }
  let photoCache = { key: '', data: null };

  function setSourceFromImage(img, name) {
    const w = img.naturalWidth || img.width, h = img.naturalHeight || img.height;
    if (!w || !h) throw new Error('image has no size');
    const c = makeCanvas(w, h);
    ctx2d(c).drawImage(img, 0, 0);
    source = { canvas: c, w, h, name };
    photoCache = { key: '', data: null };
    $('srcInfo').textContent = `${name} · ${w}×${h}`;
    render();
  }

  function loadImageUrl(url) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error('could not decode image'));
      img.src = url;
    });
  }

  async function loadFile(file, name) {
    if (!file || !(file.type || '').startsWith('image/')) { setStatus('Not an image file', 'err'); return; }
    const url = URL.createObjectURL(file);
    try {
      const img = await loadImageUrl(url);
      setSourceFromImage(img, name || file.name || 'pasted image');
      setStatus('');
    } catch (e) {
      setStatus(String(e.message || e), 'err');
    } finally {
      URL.revokeObjectURL(url);
    }
  }

  // ---- badge ---------------------------------------------------------------------------------------
  let badgeImg = null;
  const badgeCache = new Map(); // key "w,h" -> canvas

  function badgeFor(width) {
    const bw = Math.round(width);
    const bh = Math.round(width * BADGE_SVG_H / BADGE_SVG_W);
    const key = bw + ',' + bh;
    if (badgeCache.has(key)) return badgeCache.get(key);
    // Render big (vector-crisp), then step down by halves to the final size.
    const big = makeCanvas(bw * BADGE_SS, bh * BADGE_SS);
    ctx2d(big).drawImage(badgeImg, 0, 0, big.width, big.height);
    const small = resample(big, 0, 0, big.width, big.height, bw, bh);
    badgeCache.set(key, small);
    return small;
  }

  // ---- rendering -----------------------------------------------------------------------------------
  function rawPhoto() {
    // Square crop window, scaled into the 66x66 card. Cached per pan/zoom.
    const { panX, panY, zoom } = params;
    const key = [panX, panY, zoom].join('|');
    if (photoCache.key === key) return photoCache.data;

    let cropH = source.h, cropW = Math.round(cropH * CARD_ASPECT);
    if (cropW > source.w) { cropW = source.w; cropH = Math.round(cropW / CARD_ASPECT); }
    cropW = Math.max(1, Math.round(cropW / zoom));
    cropH = Math.max(1, Math.round(cropH / zoom));
    const left = Math.round((source.w - cropW) * panX);
    const top = Math.round((source.h - cropH) * panY);

    const c = resample(source.canvas, left, top, cropW, cropH, CARD_W, CARD_H);
    const data = ctx2d(c, { willReadFrequently: true }).getImageData(0, 0, CARD_W, CARD_H);
    photoCache = { key, data };
    return data;
  }

  // Saturation, contrast and unsharp mask on a copy of the raw pixels. Follows Pillow's
  // ImageEnhance.Color / Contrast (blend toward luma / mean luma, truncating) and UnsharpMask.
  function enhance(raw) {
    const n = CARD_W * CARD_H;
    const px = new Uint8ClampedArray(raw.data);
    const luma = (r, g, b) => (r * 19595 + g * 38470 + b * 7471 + 0x8000) >> 16;
    const blend = (d, v, f) => clamp255(Math.trunc(d + f * (v - d)));

    const sat = params.saturation;
    for (let i = 0; i < n; i++) {
      const o = i * 4, L = luma(px[o], px[o + 1], px[o + 2]);
      px[o] = blend(L, px[o], sat);
      px[o + 1] = blend(L, px[o + 1], sat);
      px[o + 2] = blend(L, px[o + 2], sat);
    }
    let sum = 0;
    for (let i = 0; i < n; i++) sum += luma(px[i * 4], px[i * 4 + 1], px[i * 4 + 2]);
    const mean = Math.trunc(sum / n + 0.5);
    const con = params.contrast;
    for (let i = 0; i < n; i++) {
      const o = i * 4;
      px[o] = blend(mean, px[o], con);
      px[o + 1] = blend(mean, px[o + 1], con);
      px[o + 2] = blend(mean, px[o + 2], con);
    }

    const amount = params.sharpen;
    if (amount > 0) {
      const plane = new Float32Array(n);
      for (let ch = 0; ch < 3; ch++) {
        for (let i = 0; i < n; i++) plane[i] = px[i * 4 + ch];
        const blur = blurPlane(plane, CARD_W, CARD_H, SHARPEN_SIGMA);
        for (let i = 0; i < n; i++) {
          const v = plane[i], diff = v - Math.round(blur[i]);
          if (Math.abs(diff) >= SHARPEN_THRESHOLD) px[i * 4 + ch] = clamp255(Math.trunc(v + diff * amount));
        }
      }
    }
    return px;
  }

  // Card face from the outside in: #222 outline, light line (photo toward white), the photo.
  // Each inner layer has its own rounded shape (radius shrinking with the inset), so the outline and
  // the light line follow the corner curve instead of being clipped by the card mask.
  function buildFace(px) {
    const face = new ImageData(CARD_W, CARD_H);
    const f = face.data, b = params.border, r = params.radius;
    const light = roundedAlpha(CARD_W - 2, CARD_H - 2, Math.max(r - 1, 0));
    const inner = roundedAlpha(CARD_W - 4, CARD_H - 4, Math.max(r - 2, 0));
    for (let y = 0; y < CARD_H; y++) {
      for (let x = 0; x < CARD_W; x++) {
        const o = (y * CARD_W + x) * 4;
        const a1 = x >= 1 && x < CARD_W - 1 && y >= 1 && y < CARD_H - 1
          ? light[(y - 1) * (CARD_W - 2) + x - 1] / 255 : 0;
        const a2 = x >= 2 && x < CARD_W - 2 && y >= 2 && y < CARD_H - 2
          ? inner[(y - 2) * (CARD_W - 4) + x - 2] / 255 : 0;
        for (let c = 0; c < 3; c++) {
          const lightC = Math.trunc(px[o + c] + b * (255 - px[o + c]));
          let v = 0x22 + (lightC - 0x22) * a1;
          v = v + (px[o + c] - v) * a2;
          f[o + c] = Math.round(v);
        }
        f[o + 3] = 255;
      }
    }
    return face;
  }

  // Alpha (0..255) of a w x h rounded rect with radius r, drawn at 4x and reduced for smooth corners.
  const alphaCache = new Map();
  function roundedAlpha(w, h, r) {
    const key = w + 'x' + h + '@' + r;
    if (alphaCache.has(key)) return alphaCache.get(key);
    const big = makeCanvas(w * 4, h * 4);
    const g = big.getContext('2d');
    g.fillStyle = '#000';
    g.beginPath();
    g.roundRect(0, 0, w * 4, h * 4, r * 4);
    g.fill();
    const small = resample(big, 0, 0, big.width, big.height, w, h);
    const d = ctx2d(small, { willReadFrequently: true }).getImageData(0, 0, w, h).data;
    const a = new Uint8ClampedArray(w * h);
    for (let i = 0; i < a.length; i++) a[i] = d[i * 4 + 3];
    alphaCache.set(key, a);
    return a;
  }

  // Rounded-rect card mask, drawn at 4x and reduced for smooth corners.
  const maskCache = new Map();
  function cardMask() {
    const r = params.radius;
    if (maskCache.has(r)) return maskCache.get(r);
    const big = makeCanvas(CARD_W * 4, CARD_H * 4);
    const g = big.getContext('2d');
    g.fillStyle = '#000';
    g.beginPath();
    g.roundRect(0, 0, CARD_W * 4, CARD_H * 4, r * 4);
    g.fill();
    const mask = resample(big, 0, 0, big.width, big.height, CARD_W, CARD_H);
    maskCache.set(r, mask);
    return mask;
  }

  // Darken the face under the badge's drop shadow (black, alpha from the badge, offset, blurred).
  // Pillow pastes the RGBA shadow using its own alpha as the mask, which squares the alpha;
  // this reproduces that so the output matches the reference icon.
  function applyBadgeShadow(face, badge, bx, by) {
    const strength = params.badgeShadow;
    if (strength <= 0) return;
    const pad = 4, w = CARD_W + 2 * pad, h = CARD_H + 2 * pad;
    const bd = ctx2d(badge, { willReadFrequently: true }).getImageData(0, 0, badge.width, badge.height).data;
    const plane = new Float32Array(w * h);
    for (let y = 0; y < badge.height; y++) {
      for (let x = 0; x < badge.width; x++) {
        const px = bx + x + BADGE_SHADOW_DX + pad, py = by + y + BADGE_SHADOW_DY + pad;
        if (px < 0 || py < 0 || px >= w || py >= h) continue;
        const a = Math.trunc(bd[(y * badge.width + x) * 4 + 3] * strength);
        plane[py * w + px] = Math.trunc(a * a / 255);
      }
    }
    const blur = blurPlane(plane, w, h, BADGE_SHADOW_BLUR);
    const f = face.data;
    for (let y = 0; y < CARD_H; y++) {
      for (let x = 0; x < CARD_W; x++) {
        const a = blur[(y + pad) * w + x + pad] / 255;
        if (a <= 0) continue;
        const o = (y * CARD_W + x) * 4, k = 1 - a;
        f[o] = f[o] * k; f[o + 1] = f[o + 1] * k; f[o + 2] = f[o + 2] * k;
      }
    }
  }

  function renderIcon() {
    const out = makeCanvas(FILE_W, FILE_H);
    const g = ctx2d(out);

    // Card shadow: the card's own shape, nudged down, blurred, ALPHA scaled.
    const mask = cardMask();
    const mg = ctx2d(mask, { willReadFrequently: true });
    const ma = mg.getImageData(0, 0, CARD_W, CARD_H).data;
    const sp = new Float32Array(FILE_W * FILE_H);
    for (let y = 0; y < CARD_H; y++) {
      for (let x = 0; x < CARD_W; x++) {
        sp[(y + CARD_Y + CARD_SHADOW_DY) * FILE_W + x + CARD_X] = ma[(y * CARD_W + x) * 4 + 3];
      }
    }
    const sb = blurPlane(sp, FILE_W, FILE_H, CARD_SHADOW_BLUR);
    const sd = new ImageData(FILE_W, FILE_H);
    for (let i = 0; i < sb.length; i++) sd.data[i * 4 + 3] = Math.trunc(sb[i] * params.cardShadow);
    g.putImageData(sd, 0, 0);

    // Card: face, badge shadow, badge, then the rounded mask.
    const face = buildFace(enhance(rawPhoto()));
    const badge = badgeImg ? badgeFor(params.badgeW) : null;
    const bx = badge ? Math.floor((CARD_W - badge.width) / 2) : 0;
    const by = Math.round(params.badgeTop);
    if (badge) applyBadgeShadow(face, badge, bx, by);

    const card = makeCanvas(CARD_W, CARD_H);
    const cg = ctx2d(card);
    cg.putImageData(face, 0, 0);
    if (badge) cg.drawImage(badge, bx, by);
    cg.globalCompositeOperation = 'destination-in';
    cg.drawImage(mask, 0, 0);
    g.drawImage(card, CARD_X, CARD_Y);
    return out;
  }

  // ---- previews --------------------------------------------------------------------------------------
  let currentIcon = null;
  let references = []; // [{name, img}]
  let stripMine = null;

  // Draw an icon at its native size, centered in a 96px-tall row cell.
  function drawNative(canvas, img) {
    const g = ctx2d(canvas);
    g.clearRect(0, 0, canvas.width, canvas.height);
    g.drawImage(img, 0, 0);
  }

  function buildStrip() {
    const strip = $('strip');
    strip.textContent = '';
    stripMine = null;
    const mk = (title, w, h) => {
      const c = makeCanvas(w, h);
      c.title = title;
      return c;
    };
    const addMine = () => {
      stripMine = mk('Your icon', FILE_W, FILE_H);
      stripMine.className = 'mine';
      strip.appendChild(stripMine);
    };
    const slot = Math.min(3, references.length);
    references.forEach((ref, i) => {
      if (i === slot) addMine();
      const w = ref.img.naturalWidth || ref.img.width, h = ref.img.naturalHeight || ref.img.height;
      const c = mk(ref.name, w, h);
      drawNative(c, ref.img);
      strip.appendChild(c);
    });
    if (!stripMine) addMine();
  }

  let renderQueued = false;
  function render() {
    if (renderQueued) return;
    renderQueued = true;
    requestAnimationFrame(() => { renderQueued = false; renderNow(); });
  }

  function renderNow() {
    if (!source) return;
    const icon = renderIcon();
    currentIcon = icon;

    const fp = $('filePreview'), fg = fp.getContext('2d');
    fg.clearRect(0, 0, FILE_W, FILE_H);
    fg.drawImage(icon, 0, 0);

    for (const id of ['cam1', 'cam3']) {
      const c = $(id), g = ctx2d(c);
      g.fillStyle = '#1c1c1c';
      g.fillRect(0, 0, c.width, c.height);
      g.drawImage(icon, 0, 0, c.width, c.height);
    }
    if (stripMine) drawNative(stripMine, icon);
    document.body.dataset.ready = '1';
  }

  // ---- UI --------------------------------------------------------------------------------------------
  function buildSliders() {
    const host = $('sliders');
    for (const p of PARAMS) {
      const row = document.createElement('div');
      row.className = 'slider' + (p.group ? ' group' : '');
      const label = document.createElement('label');
      label.htmlFor = 'p_' + p.id;
      label.textContent = p.label;
      const input = document.createElement('input');
      input.type = 'range';
      input.id = 'p_' + p.id;
      input.min = p.min; input.max = p.max; input.step = p.step;
      input.value = params[p.id];
      const out = document.createElement('output');
      out.id = 'o_' + p.id;
      out.htmlFor = input.id;
      out.textContent = params[p.id].toFixed(p.dp);
      input.addEventListener('input', () => {
        params[p.id] = parseFloat(input.value);
        out.textContent = params[p.id].toFixed(p.dp);
        saveParams();
        render();
      });
      row.append(label, input, out);
      host.appendChild(row);
    }
  }

  function syncSliders() {
    for (const p of PARAMS) {
      $('p_' + p.id).value = params[p.id];
      $('o_' + p.id).textContent = params[p.id].toFixed(p.dp);
    }
  }

  function setStatus(text, kind) {
    const s = $('status');
    s.textContent = text;
    s.className = 'status' + (kind ? ' ' + kind : '');
  }

  function iconBlob() {
    if (!currentIcon) return Promise.reject(new Error('no image loaded'));
    return new Promise((resolve, reject) =>
      currentIcon.toBlob((b) => (b ? resolve(b) : reject(new Error('PNG encode failed'))), 'image/png'));
  }

  function stamp() {
    const d = new Date(), z = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}${z(d.getMonth() + 1)}${z(d.getDate())}-${z(d.getHours())}${z(d.getMinutes())}${z(d.getSeconds())}`;
  }

  async function download() {
    try {
      const blob = await iconBlob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `aspect-icon-${stamp()}.png`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      setStatus(`Downloaded ${a.download}`, 'ok');
    } catch (e) {
      setStatus(String(e.message || e), 'err');
    }
  }

  async function post(path, button, describe) {
    button.disabled = true;
    setStatus('Saving…');
    try {
      const blob = await iconBlob();
      const res = await fetch(path, { method: 'POST', headers: { 'Content-Type': 'image/png' }, body: blob });
      const text = await res.text();
      let data = null;
      try { data = JSON.parse(text); } catch (e) { /* not JSON */ }
      if (!res.ok) throw new Error((data && (data.error || data.message)) || text || `HTTP ${res.status}`);
      setStatus(describe(data || {}), 'ok');
    } catch (e) {
      setStatus('Failed: ' + String(e.message || e), 'err');
    } finally {
      button.disabled = false;
    }
  }

  function wireUi() {
    buildSliders();
    $('resetBtn').addEventListener('click', () => {
      for (const p of PARAMS) params[p.id] = p.def;
      syncSliders();
      saveParams();
      render();
    });
    $('downloadBtn').addEventListener('click', download);
    $('iterBtn').addEventListener('click', (e) =>
      post('/api/iteration', e.currentTarget, (d) => `Archived: ${d.archived}`));
    $('appBtn').addEventListener('click', (e) =>
      post('/api/app-icon', e.currentTarget, (d) => `Saved: ${d.saved}\nArchived: ${d.archived}`));

    $('fileInput').addEventListener('change', (e) => {
      const f = e.target.files && e.target.files[0];
      if (f) loadFile(f);
      e.target.value = '';
    });

    let dragDepth = 0;
    window.addEventListener('dragover', (e) => e.preventDefault());
    window.addEventListener('dragenter', (e) => { e.preventDefault(); dragDepth++; document.body.classList.add('drag'); });
    window.addEventListener('dragleave', () => { if (--dragDepth <= 0) { dragDepth = 0; document.body.classList.remove('drag'); } });
    window.addEventListener('drop', (e) => {
      e.preventDefault();
      dragDepth = 0;
      document.body.classList.remove('drag');
      const f = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
      if (f) loadFile(f);
    });
    window.addEventListener('paste', (e) => {
      const items = (e.clipboardData && e.clipboardData.items) || [];
      for (const it of items) {
        if (it.kind === 'file' && it.type.startsWith('image/')) {
          const f = it.getAsFile();
          if (f) { loadFile(f, 'pasted image'); e.preventDefault(); return; }
        }
      }
    });
    $('status').style.whiteSpace = 'pre-line';
  }

  async function loadAssets() {
    try {
      badgeImg = await loadImageUrl('/assets/badge.svg');
    } catch (e) {
      setStatus('Could not load /assets/badge.svg: ' + e.message, 'err');
    }
    try {
      const res = await fetch('/assets/references.json');
      if (res.ok) {
        const list = await res.json();
        const loaded = await Promise.all(list.map((r) =>
          loadImageUrl(r.url).then((img) => ({ name: r.name, img })).catch(() => null)));
        references = loaded.filter(Boolean);
      }
    } catch (e) { /* references are optional */ }
    buildStrip();
    try {
      const img = await loadImageUrl('/assets/sample.png');
      setSourceFromImage(img, 'sample.png');
    } catch (e) {
      $('srcInfo').textContent = 'no image yet: choose, drop or paste one';
    }
    render();
  }

  loadParams();
  wireUi();
  loadAssets();
})();
