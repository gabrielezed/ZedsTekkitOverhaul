/* Original procedural calls. Run from any directory with Node.js. */
const fs = require('node:fs');
const path = require('node:path');
const output = path.resolve(__dirname, '../src/minecraft/zed/sounds');
const electrical = process.argv.includes('--jetpack');
const rate = 22050;
const tau = 2 * Math.PI;
const electricalClips = [
    ['warning', 0.53, 83, 69, 52, 0.28, 1.1, 0.85, 0.54, 0x0f7],
    ['fault', 0.38, 740, -550, 121, 0.81, 1.5, 0.36, 0.69, 0x197],
    ['recovery', 0.31, 184, 210, 29, 0.08, 0.15, 0.65, 0.43, 0x2cc],
];
// Seconds, variable throat pulse, breath mix, level, seed, [vocal-tract Hz, bandwidth, weight].
const fuglClips = [
    ['ambient1', 0.94,  94, 0.67, 0.43, 0x21d, [[520, 185, 1], [1270, 280, 0.57], [2430, 450, 0.24]]],
    ['ambient2', 1.13,  78, 0.77, 0.41, 0x827, [[410, 165, 1], [1080, 250, 0.61], [2280, 380, 0.22]]],
    ['warning',  0.56, 145, 0.74, 0.67, 0x74a, [[720, 240, 1], [1670, 325, 0.87], [2960, 520, 0.40]]],
    ['dive',     0.43,  51, 0.96, 0.48, 0x158, [[410, 340, 1], [1530, 670, 0.62], [3200, 820, 0.27]]],
    ['hurt',     0.24, 119, 0.70, 0.6,  0x69c, [[610, 230, 1], [2080, 390, 0.83], [3280, 560, 0.29]]],
    ['death',    0.83,  60, 0.80, 0.57, 0x381, [[350, 180, 1], [930, 330, 0.85], [1930, 650, 0.46]]],
];

function fuglVoice([name, seconds, rateOfPulse, breath, level, seed, bands]) {
    let rng = seed;
    function noise() {
        rng = (Math.imul(rng, 1664525) + 1013904223) >>> 0;
        return rng / 0x80000000 - 1;
    }
    const resonances = bands.map(([frequency, bandwidth, weight]) => {
        const radius = Math.exp(-Math.PI * bandwidth / rate);
        return { gain: weight * (1 - radius), feedback: 2 * radius * Math.cos(tau * frequency / rate),
            decay: radius * radius, last: 0, older: 0 };
    });
    let untilPulse = 0, throat = 0, breathNoise = 0, pressure = 0;
    const rise = name === 'dive' ? 0.20 : name.startsWith('ambient') ? 0.085 : 0.022;
    return (time, progress) => {
        if (untilPulse-- <= 0) {
            untilPulse = rate / rateOfPulse * (1.10 + 0.68 * noise() + 0.55 * progress);
            throat = 0.18 + 0.38 * Math.abs(noise());
        }
        const air = noise();
        breathNoise += (air - breathNoise) * 0.19;
        pressure += (air - pressure) * 0.0011;
        const flow = 0.77 + 0.22 * Math.tanh(pressure * 45);
        throat *= 0.85;
        const excitation = (air - breathNoise) * breath * flow + throat * (1 - breath);
        let voice = 0;
        for (const band of resonances) {
            const sample = band.gain * excitation + band.feedback * band.last - band.decay * band.older;
            band.older = band.last;
            band.last = sample;
            voice += sample;
        }
        const envelope = Math.min(1, time / rise) * Math.min(1, (seconds - time) / 0.065)
            * ((1 - progress) ** (name === 'dive' ? 0.6 : 1.25));
        return level * envelope * Math.tanh(voice * 4 + 0.17 * excitation);
    };
}

const directory = path.join(output, electrical ? "jetpack" : "aerial");
fs.mkdirSync(directory, { recursive: true });
for (const clip of electrical ? electricalClips : fuglClips) {
    const [name, seconds, base, slide, flutter, hiss, rasp, resonance, level, seed] = clip;
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
    const voice = electrical ? null : fuglVoice(clip);
    let rng = seed, phase = 0, lowNoise = 0, highNoise = 0;
    for (let i = 0; i < count; i++) {
        const time = i / rate, progress = i / count;
        if (voice) {
            wav.writeInt16LE(Math.round(voice(time, progress) * 32767), 44 + i * 2);
            continue;
        }
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
    fs.writeFileSync(path.join(directory, name + '.wav'), wav);
}
