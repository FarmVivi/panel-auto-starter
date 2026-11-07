// Ping de statut Minecraft : node ping.cjs <hote> <port> [protocole] [hote-virtuel]. Le 4e argument vise un forced host.
const net = require('net');
const [host, port, proto] = [process.argv[2], +process.argv[3], +(process.argv[4] || 775)];
const varint = n => { const b = []; do { let x = n & 0x7f; n >>>= 7; if (n) x |= 0x80; b.push(x); } while (n); return Buffer.from(b); };
const pkt = (id, data) => { const body = Buffer.concat([varint(id), data]); return Buffer.concat([varint(body.length), body]); };
const str = s => { const b = Buffer.from(s, 'utf8'); return Buffer.concat([varint(b.length), b]); };
const sock = net.connect(port, host, () => {
  const p = Buffer.alloc(2); p.writeUInt16BE(port);
  sock.write(pkt(0, Buffer.concat([varint(proto), str(process.argv[5] || host), p, varint(1)])));
  sock.write(pkt(0, Buffer.alloc(0)));
});
let buf = Buffer.alloc(0);
const readVarint = (b, o) => { let n = 0, s = 0, x; do { x = b[o++]; n |= (x & 0x7f) << s; s += 7; } while (x & 0x80); return [n, o]; };
sock.on('data', d => {
  buf = Buffer.concat([buf, d]);
  try {
    let [len, o] = readVarint(buf, 0); if (buf.length < o + len) return;
    let [, o2] = readVarint(buf, o); let [sl, o3] = readVarint(buf, o2);
    const j = JSON.parse(buf.slice(o3, o3 + sl).toString('utf8'));
    if (j.favicon) j.favicon = j.favicon.slice(0, 30) + '...';
    console.log(JSON.stringify(j)); sock.end(); process.exit(0);
  } catch (e) { }
});
sock.on('error', e => { console.log('ERR ' + e.message); });
setTimeout(() => { console.log('TIMEOUT'); process.exit(1); }, 8000);
