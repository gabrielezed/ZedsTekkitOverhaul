/* Original procedural calls. Run from any directory with Node.js. */
const fs = require('node:fs');
const path = require('node:path');
const output = path.resolve(__dirname, '../src/minecraft/zed/sounds/aerial');
const rate = 22050;
const tau = 2 * Math.PI;
const clips = [
    ['ambient1', 0.94, 210, -112, 12, 0.26, 0.24, 0.48, 0.43, 0x21d],
    ['ambient2', 1.13, 178, -74, 9, 0.30, 0.29, 0.45, 0.48, 0x827],
    ['warning',  0.56, 322, 354, 34, 0.14, 0.78, 0.65, 0.74, 0x74a],
    ['dive',     0.43, 503,-358, 19, 0.65, 0.63, 0.35, 0.56, 0x158],
    ['hurt',     0.24, 344,-185, 45, 0.44, 0.95, 0.40, 0.63, 0x69c],
    ['death',    0.83, 256,-191, 22, 0.35, 0.58, 0.74, 0.61, 0x381],
];
fs.mkdirSync(output, { recursive: true });
for (const [name, seconds, base, slide, flutter, hiss, rasp, resonance, level, seed] of clips) {
    const count = Math.round(seconds * rate);
    const wav = Buffer.alloc(44 + count * 2);
    wav.write('RIFF', 0);
    wav.writeUInt32LE(wav.length - 8, 4);
    wav.write('WAVEfmt ', 8);
    wav.writeUInt32LE(16, 16);
    wav.writeUInt16LE(1, 20);
    wav.writeUInt16LE(1, 22);
    wav.writeUInt32LE(rate, 24);
    wav.writeUInt32LE(rate * 2, 28);
    wav.writeUInt16LE(2, 32);
    wav.writeUInt16LE(16, 34);
    wav.write('data', 36);
    wav.writeUInt32LE(count * 2, 40);
    let rng = seed, phase = 0, lowNoise = 0, highNoise = 0;
    for (let i = 0; i < count; i++) {
        const time = i / rate, progress = i / count;
        rng = (Math.imul(rng, 1664525) + 1013904223) >>> 0;
        const noise = rng / 0x80000000 - 1;
        lowNoise += (noise - lowNoise) * 0.035;
        highNoise += (noise - highNoise) * 0.28;
        const tremor = Math.sin(tau * flutter * time);
        const frequency = base + slide * progress ** 0.68 + base * (0.047 * tremor + 0.035 * lowNoise);
        phase += tau * frequency / rate;
        const pressure = Math.sin(phase + Math.sin(phase * 0.506) * rasp);
        const formants = Math.sin(phase * 2.015 + 0.6) * 0.46 + Math.sin(phase * 3.04) * 0.15;
        const pulse = name.startsWith('ambient') ? 0.3 + 0.7 * Math.sin(7.2 * progress + 0.45) ** 2 : 1;
        const decay = name === 'warning' ? 0.55 : 1.35;
        const envelope = Math.min(1, time / 0.027) * Math.min(1, (seconds - time) / 0.07)
            * (1 - progress) ** decay * pulse;
        const signal = pressure * (1 - hiss) + formants * resonance
            + (highNoise - lowNoise) * hiss * (name === 'dive' ? 1.8 : 0.8);
        const sample = level * envelope * Math.tanh(signal * (1.55 + rasp)) * (0.83 + tremor * rasp * 0.17);
        wav.writeInt16LE(Math.round(sample * 32767), 44 + i * 2);
    }
    fs.writeFileSync(path.join(output, name + '.wav'), wav);
}
