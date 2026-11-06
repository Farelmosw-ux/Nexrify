// Inti Global Chat: dipakai /api/chat (pengguna) dan /api/admin (pengumuman, laporan, blokir)
import { redis, redisMulti, clip, getUser } from './_lib.js'
import { censor } from './_chatfilter.js'

export const K = {
  seq: 'nx:chat:seq', evSeq: 'nx:chat:ev:seq', ev: 'nx:chat:ev', idx: 'nx:chat:idx', typing: 'nx:chat:typing',
  m: s => 'nx:chat:m:' + s, st: id => 'nx:chat:st:' + id, n: id => 'nx:chat:n:' + id,
  ann: 'nx:chat:ann', rep: 'nx:chat:rep', ban: id => 'nx:chat:ban:' + id, unx: 'nx:chat:unx',
  roles: 'nx:chat:roles', group: 'nx:chat:group', presence: 'nx:chat:presence'
}
export const ONLINE_MS = 150000
export const GROUP_DEFAULT = { name: 'Global Chat', desc: 'Ruang obrolan publik untuk semua pengguna Nexrify.' }
export const EMOJIS = ['👍', '😂', '😮', '😭', '🙏', '😁', '😡', '❌', '✅', '❤️', '🗿']
export const MAX_TEXT = 500
export const EDIT_WINDOW = 3600e3
const TTL = 14 * 86400
export const MENTION_RE = /(?:^|[^A-Za-z0-9_])@([a-z0-9_]{3,20})/gi

export const cleanText = (t, max = MAX_TEXT) =>
  censor(clip(String(t ?? '').replace(/\r\n?/g, '\n'), max).replace(/\n{3,}/g, '\n\n')).text.trim()

export const parseMsg = raw => { try { return raw ? JSON.parse(raw) : null } catch { return null } }
export const loadMsg = async seq => (Number.isInteger(+seq) && +seq > 0) ? parseMsg(await redis('GET', K.m(+seq))) : null

export const shape = m => ({
  seq: m.s, uid: m.u, username: m.n, display: m.d, badges: m.bd || [], av: m.av || '',
  text: m.x ? '' : m.t, ts: m.ts, edited: m.e || 0, deleted: !!m.x, announce: m.k === 'a',
  mentions: m.mn || [], reply: m.r ? { seq: m.r.s, username: m.r.n, text: m.r.t } : null,
  // Reaksi lama yang sudah tidak ada di daftar (mis. 🔥 🎵) tidak ditampilkan lagi
  reactions: Object.fromEntries(Object.entries(m.rx || {}).filter(([e, u]) => EMOJIS.includes(e) && u.length))
})

const envList = k => new Set(String(process.env[k] || '').toLowerCase().split(',').map(x => x.trim()).filter(Boolean))
/** Peran dari environment (CHAT_ADMIN, CHAT_STAFF). Peran yang diatur lewat panel disimpan di Redis dan menang atas ini. */
export const envRole = username => envList('CHAT_ADMIN').has(username) ? 'admin' : envList('CHAT_STAFF').has(username) ? 'staff' : ''
export async function roleOf(username) { return (await redis('HGET', K.roles, username)) || envRole(username) }
export const canManage = role => role === 'admin' || role === 'staff'

export function badgeList(username, st = {}, role = '') {
  const b = []
  if (role === 'admin') b.push('admin')
  else if (role === 'staff') b.push('staff')
  if (st.early) b.push('early')
  if (+st.msgs >= 100) b.push('active')
  if (+st.rx >= 25) b.push('loved')
  return b
}

// Tanda kehadiran: ditulis paling sering sekali per 30 detik per pengguna per instance supaya hemat kuota.
const presenceMemo = new Map()
export async function touchPresence(uid) {
  const now = Date.now()
  if (now - (presenceMemo.get(uid) || 0) < 30000) return
  presenceMemo.set(uid, now)
  if (presenceMemo.size > 2000) presenceMemo.clear()
  const cmds = [['ZADD', K.presence, now, uid]]
  if (Math.random() < 0.05) cmds.push(['ZREMRANGEBYSCORE', K.presence, '-inf', now - 86400000])
  await redisMulti(cmds)
}

export async function setRole(username, role) {
  const name = clip(username, 30).toLowerCase().replace(/^@/, '')
  if (!(await redis('GET', 'nx:uname:' + name))) throw new Error('Username tidak ditemukan')
  if (role === 'admin' || role === 'staff') await redis('HSET', K.roles, name, role)
  else await redis('HDEL', K.roles, name)
}

export const snippet = t => String(t || '').replace(/\s+/g, ' ').slice(0, 60)

/** Tulis ulang pesan yang berubah dan catat sebagai kejadian baru supaya klien mengambilnya di polling berikutnya. */
export async function saveMsg(m, extra = []) {
  const [ev] = await redisMulti([['INCR', K.evSeq]])
  await redisMulti([
    ['SET', K.m(m.s), JSON.stringify(m), 'EX', TTL],
    ['ZADD', K.ev, ev, m.s],
    ['ZREMRANGEBYRANK', K.ev, 0, -601],
    ...extra
  ])
}

/** Buat pesan baru. u = pengguna (atau { sys: true } untuk pengumuman). Mengembalikan pesan berbentuk publik. */
export async function createMessage({ u, text, replyTo, announce = false, av = '' }) {
  const now = Date.now()
  const sys = announce
  const uid = sys ? 'sys' : u.id
  const pre = [['INCR', K.seq], ['INCR', K.evSeq]]
  if (!sys) pre.push(['HINCRBY', K.st(uid), 'msgs', 1], ['HGETALL', K.st(uid)], ['HGET', K.roles, u.username])
  const out = await redisMulti(pre)
  const seq = out[0], ev = out[1]
  let st = {}
  if (!sys) { const a = out[3] || []; for (let i = 0; i < a.length; i += 2) st[a[i]] = a[i + 1] }

  let r = null
  if (replyTo) {
    const o = await loadMsg(replyTo)
    if (o) r = { s: o.s, u: o.u, n: o.n, t: o.x ? '' : snippet(o.t) }
  }
  const names = [...new Set([...text.matchAll(MENTION_RE)].map(x => x[1].toLowerCase()))].slice(0, 5)
  let mentioned = []
  if (names.length) {
    const ids = await redis('MGET', ...names.map(n => 'nx:uname:' + n))
    mentioned = names.map((n, i) => ({ n, id: ids?.[i] })).filter(x => x.id && x.id !== uid)
  }
  const m = {
    s: seq, u: uid, n: sys ? 'nexrify' : u.username, d: sys ? 'Nexrify' : u.display, av,
    bd: sys ? ['admin'] : badgeList(u.username, st, out[4] || envRole(u.username)), t: text, r, ts: now, k: sys ? 'a' : 'm',
    mn: mentioned.map(x => x.n), rx: {}
  }
  const cmds = [
    ['SET', K.m(seq), JSON.stringify(m), 'EX', TTL],
    ['ZADD', K.idx, seq, seq], ['ZADD', K.ev, ev, seq],
    ['ZREMRANGEBYRANK', K.idx, 0, -501], ['ZREMRANGEBYRANK', K.ev, 0, -601]
  ]
  const notified = new Set()
  const notify = (id, k) => {
    if (!id || id === uid || notified.has(id)) return
    notified.add(id)
    cmds.push(['ZADD', K.n(id), now, `${k}|${seq}|${m.n}|${snippet(text)}`], ['ZREMRANGEBYRANK', K.n(id), 0, -31])
  }
  for (const x of mentioned) notify(x.id, 'm')
  if (r && r.u && r.u !== 'sys') notify(r.u, 'r')
  if (sys) cmds.push(['ZADD', K.ann, now, `${seq}|${snippet(text)}`], ['ZREMRANGEBYRANK', K.ann, 0, -11])
  await redisMulti(cmds)
  return { ev, message: shape(m) }
}

export async function deleteMessage(seq, by) {
  const m = await loadMsg(seq)
  if (!m || m.x) return null
  m.x = 1; m.t = ''; m.rx = {}
  await saveMsg(m)
  return m
}

/* ---------- moderasi (dipakai panel admin) ---------- */
export const announce = text => createMessage({ text: cleanText(text, 400), announce: true })
export async function listReports() {
  const rows = await redis('ZREVRANGE', K.rep, 0, 99)
  return (rows || []).map(parseMsg).filter(Boolean)
}
export const clearReport = async id => {
  const rows = await redis('ZRANGE', K.rep, 0, -1)
  for (const r of rows || []) { const o = parseMsg(r); if (o?.id === id) await redis('ZREM', K.rep, r) }
}
export async function banUser(username, hours) {
  const name = clip(username, 30).toLowerCase().replace(/^@/, '')
  const id = await redis('GET', 'nx:uname:' + name)
  if (!id) throw new Error('Username tidak ditemukan')
  if (hours > 0) await redis('SET', K.ban(id), '1', 'EX', Math.floor(hours * 3600))
  else await redis('SET', K.ban(id), '1')
  return id
}
export async function unbanUser(username) {
  const id = await redis('GET', 'nx:uname:' + clip(username, 30).toLowerCase().replace(/^@/, ''))
  if (!id) throw new Error('Username tidak ditemukan')
  await redis('DEL', K.ban(id))
}
