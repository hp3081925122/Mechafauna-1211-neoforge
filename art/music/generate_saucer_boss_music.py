# 飞碟母舰 Boss 音乐程序化合成脚本，输出 OGG 到模组资源目录
import os
import subprocess
import wave
import numpy as np

SR = 44100
BPM = 140
BEAT = 60.0 / BPM
STEP = BEAT / 4.0
BAR = BEAT * 4.0
TOTAL_BARS = 64
LENGTH = int(TOTAL_BARS * BAR * SR)
RNG = np.random.default_rng(20260926)
HERE = os.path.dirname(os.path.abspath(__file__))
OUTPUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "mechafauna", "sounds", "music", "flying_saucer_boss.ogg")

# 各声部独立母线，最后统一混音
dry = np.zeros((2, LENGTH))
send = np.zeros((2, LENGTH))
arp_bus = np.zeros((2, LENGTH))


def freq(midi):
    # MIDI 音高转频率
    return 440.0 * 2.0 ** ((midi - 69) / 12.0)


def place(bus, start, signal, gain=1.0, pan=0.0):
    # 按等功率声像把单声道信号写入母线
    index = int(start * SR)
    if index >= LENGTH:
        return
    end = min(LENGTH, index + len(signal))
    part = signal[:end - index] * gain
    left = np.cos((pan + 1.0) * np.pi / 4.0)
    right = np.sin((pan + 1.0) * np.pi / 4.0)
    bus[0, index:end] += part * left
    bus[1, index:end] += part * right


def highpass(signal, width=6):
    # 减去滑动平均作为简易高通
    kernel = np.ones(width) / width
    return signal - np.convolve(signal, kernel, mode="same")


def envelope(length, attack, release, sustain_time):
    # 线性起音、保持、指数释放
    t = np.arange(length) / SR
    env = np.minimum(1.0, t / max(attack, 1e-4))
    tail = np.clip(t - sustain_time, 0.0, None)
    return env * np.exp(-tail / max(release, 1e-4))


def saw(f, duration, cutoff, harmonics=14, detune=(0.0,)):
    # 加法合成锯齿波，谐波按截止频率衰减模拟低通
    n = int(duration * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), (n,))
    for cents in detune:
        base = f * 2.0 ** (cents / 1200.0)
        phase = RNG.random() * 2.0 * np.pi
        for k in range(1, harmonics + 1):
            if base * k > SR * 0.45:
                break
            weight = 1.0 / (1.0 + (base * k / cutoff) ** 4)
            out += weight * np.sin(2.0 * np.pi * base * k * t + phase * k) / k
    return out / len(detune)


def square(f, duration, harmonics=9):
    # 奇次谐波方波，用于琶音
    n = int(duration * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for k in range(1, harmonics + 1, 2):
        out += np.sin(2.0 * np.pi * f * k * t) / k
    return out

def kick(start, gain=1.0):
    # 频率下滑的正弦底鼓，带短促咔嗒声
    n = int(0.45 * SR)
    t = np.arange(n) / SR
    f = 45.0 + 110.0 * np.exp(-t / 0.035)
    body = np.sin(2.0 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.16)
    click = RNG.standard_normal(n) * np.exp(-t / 0.004) * 0.3
    place(dry, start, np.tanh((body + click) * 1.6), 0.9 * gain)


def snare(start, gain=1.0):
    # 噪声加 185Hz 音体的军鼓，送入混响
    n = int(0.3 * SR)
    t = np.arange(n) / SR
    noise = highpass(RNG.standard_normal(n), 3) * np.exp(-t / 0.07)
    tone = np.sin(2.0 * np.pi * 185.0 * t) * np.exp(-t / 0.05)
    signal = noise * 0.55 + tone * 0.5
    place(dry, start, signal, 0.55 * gain)
    place(send, start, signal, 0.25 * gain)


def hat(start, gain=1.0, open_hat=False):
    # 高通噪声踩镲，开镲衰减更长
    decay = 0.018
    if open_hat:
        decay = 0.12
    n = int((decay * 6.0 + 0.01) * SR)
    t = np.arange(n) / SR
    signal = highpass(RNG.standard_normal(n), 2) * np.exp(-t / decay)
    place(dry, start, signal, 0.11 * gain, RNG.uniform(-0.3, 0.3))


def crash(start, gain=1.0):
    # 长衰减镲片
    n = int(2.2 * SR)
    t = np.arange(n) / SR
    signal = highpass(RNG.standard_normal(n), 2) * np.exp(-t / 0.7)
    place(dry, start, signal, 0.14 * gain, -0.2)
    place(send, start, signal, 0.12 * gain, 0.2)


def impact(start, gain=1.0):
    # 下潜低频冲击，用于段落开头
    n = int(2.4 * SR)
    t = np.arange(n) / SR
    f = 30.0 + 90.0 * np.exp(-t / 0.3)
    body = np.sin(2.0 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.9)
    place(dry, start, np.tanh(body * 1.4), 0.7 * gain)
    place(send, start, body, 0.2 * gain)
    crash(start, gain)


def riser(start, duration, gain=1.0):
    # 上升噪声与正弦滑音，段落前的蓄力
    n = int(duration * SR)
    t = np.arange(n) / SR
    progress = t / duration
    noise = highpass(RNG.standard_normal(n), 2) * progress ** 2
    f = 200.0 * 2.0 ** (progress * 4.0)
    sweep = np.sin(2.0 * np.pi * np.cumsum(f) / SR) * progress ** 1.5
    place(dry, start, noise * 0.12 + sweep * 0.08, gain)
    place(send, start, noise * 0.1 + sweep * 0.1, gain)


def bass(start, midi, duration, gain=1.0):
    # 锯齿低音叠加正弦次低音，截止频率随包络快速关闭
    n = int(duration * SR)
    t = np.arange(n) / SR
    cutoff = 180.0 + 1400.0 * np.exp(-t / 0.05)
    f = freq(midi)
    signal = saw(f, duration, cutoff, 16, (-6.0, 6.0))
    sub = np.sin(2.0 * np.pi * f * t) * 0.8
    env = envelope(n, 0.003, 0.05, duration * 0.6)
    place(dry, start, np.tanh((signal + sub) * env * 1.5), 0.42 * gain)


def pad(start, midis, duration, cutoff=900.0, gain=1.0):
    # 慢起音的失谐锯齿和弦垫，大部分送入混响
    n = int(duration * SR)
    env = envelope(n, duration * 0.35, 0.6, duration * 0.8)
    for index, midi in enumerate(midis):
        signal = saw(freq(midi), duration, cutoff, 8, (-9.0, 0.0, 9.0)) * env
        pan = (index / max(1, len(midis) - 1) - 0.5) * 0.9
        place(dry, start, signal, 0.07 * gain, pan)
        place(send, start, signal, 0.1 * gain, -pan)


def arp_note(start, midi, gain=1.0, pan=0.0, brightness=1.0):
    # 短促方波琶音，送入延迟母线
    duration = STEP * 0.9
    n = int(duration * SR)
    env = envelope(n, 0.002, 0.04, 0.01)
    signal = square(freq(midi), duration, int(3 + 8 * brightness)) * env
    place(dry, start, signal, 0.07 * gain, pan)
    place(arp_bus, start, signal, 0.05 * gain, -pan)


def lead(start, midi, duration, gain=1.0):
    # 带延迟颤音的失谐锯齿主旋律，叠一层低八度
    n = int(duration * SR)
    t = np.arange(n) / SR
    depth = np.clip((t - 0.15) / 0.3, 0.0, 1.0) * 0.006
    vibrato = 1.0 + depth * np.sin(2.0 * np.pi * 5.5 * t)
    env = envelope(n, 0.01, 0.12, duration * 0.85)
    signal = np.zeros(n)
    for cents, weight in ((-7.0, 0.5), (7.0, 0.5), (-1200.0, 0.35)):
        f = freq(midi) * 2.0 ** (cents / 1200.0) * vibrato
        phase = 2.0 * np.pi * np.cumsum(f) / SR
        for k in range(1, 12):
            if freq(midi) * k > SR * 0.4:
                break
            rolloff = 1.0 / (1.0 + (freq(midi) * k / 3200.0) ** 4)
            signal += weight * rolloff * np.sin(phase * k) / k
    signal = np.tanh(signal * env * 1.2)
    place(dry, start, signal, 0.14 * gain)
    place(arp_bus, start, signal, 0.06 * gain)
    place(send, start, signal, 0.08 * gain)


def scanner(start, gain=1.0):
    # 外星扫描哔声，两音交替
    for index, midi in enumerate((93, 98, 93, 98)):
        n = int(0.09 * SR)
        t = np.arange(n) / SR
        signal = np.sin(2.0 * np.pi * freq(midi) * t) * envelope(n, 0.002, 0.03, 0.03)
        place(dry, start + index * STEP * 2.0, signal, 0.06 * gain, 0.5 - index % 2)
        place(send, start + index * STEP * 2.0, signal, 0.08 * gain)

# D 小调和声进行：Dm - Bb - Gm - A，每小节一个和弦
ROOTS = [38, 34, 31, 33]
CHORDS = [[62, 65, 69], [58, 62, 65], [55, 58, 62], [57, 61, 64]]
ARP_CHORDS = [[74, 77, 81], [70, 74, 77], [67, 70, 74], [69, 73, 76]]
ARP_ORDER = [0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 4, 3, 2, 1, 2, 1]
BASS_PATTERN = [0, None, 0, 12, 0, None, 0, 12, 0, None, 0, 12, 0, 0, 12, 0]
# 八小节主旋律：（小节, 起始十六分音符, 长度, 音高）
MELODY = [
    (0, 0, 6, 74), (0, 6, 2, 77), (0, 8, 4, 76), (0, 12, 4, 74),
    (1, 0, 6, 77), (1, 6, 2, 81), (1, 8, 8, 82),
    (2, 0, 6, 79), (2, 6, 2, 77), (2, 8, 4, 74), (2, 12, 4, 70),
    (3, 0, 8, 69), (3, 8, 4, 73), (3, 12, 4, 76),
    (4, 0, 6, 81), (4, 6, 2, 79), (4, 8, 4, 77), (4, 12, 4, 76),
    (5, 0, 6, 74), (5, 6, 2, 77), (5, 8, 6, 82), (5, 14, 2, 81),
    (6, 0, 4, 79), (6, 4, 4, 82), (6, 8, 8, 86),
    (7, 0, 8, 85), (7, 8, 4, 81), (7, 12, 4, 76),
]


def section(bar):
    # 段落划分：前奏、推进、主旋律、间奏、高潮
    if bar < 8:
        return "intro"
    if bar < 24:
        return "drive"
    if bar < 40:
        return "theme"
    if bar < 48:
        return "break"
    return "climax"


def arp_value(chord, order):
    # 琶音序号超出和弦音时向上叠八度
    return chord[order % 3] + 12 * (order // 3)


for bar in range(TOTAL_BARS):
    start = bar * BAR
    part = section(bar)
    chord = bar % 4
    # 和弦垫贯穿全曲，间奏更暗
    cutoff = 1100.0
    if part in ("intro", "break"):
        cutoff = 600.0 + 60.0 * (bar % 8)
    pad(start, CHORDS[chord] + [CHORDS[chord][0] - 12], BAR * 1.05, cutoff, 1.0)
    # 段落开头的冲击和前一段末尾的蓄力
    if bar in (8, 24, 40, 48):
        impact(start, 1.0)
    if bar in (6, 22, 46):
        riser(start, BAR * 2.0, 1.0)
    if bar % 8 == 0:
        scanner(start + BEAT * 2.0, 1.0)
    # 琶音：前奏后半开始，间奏保留
    if part != "intro" or bar >= 4:
        brightness = 0.4
        if part in ("theme", "climax"):
            brightness = 1.0
        for step in range(16):
            order = ARP_ORDER[step]
            pan = 0.35
            if step % 2 == 1:
                pan = -0.35
            arp_note(start + step * STEP, arp_value(ARP_CHORDS[chord], order), 1.0, pan, brightness)
    # 低音：推进段起全程十六分音符
    if part in ("drive", "theme", "climax"):
        for step, offset in enumerate(BASS_PATTERN):
            if offset is None:
                continue
            bass(start + step * STEP, ROOTS[chord] + offset, STEP * 0.85, 1.0)
    elif part == "break" and bar >= 44:
        bass(start, ROOTS[chord], BAR * 0.9, 0.8)
    # 鼓组
    if part in ("drive", "theme", "climax"):
        for step in (0, 4, 8, 12):
            kick(start + step * STEP)
        if bar % 2 == 1:
            kick(start + 14 * STEP, 0.7)
        snare(start + 4 * STEP)
        snare(start + 12 * STEP)
        if bar % 4 == 3:
            for step in (13, 14, 15):
                snare(start + step * STEP, 0.35 + 0.15 * (step - 13))
        for step in range(16):
            if part == "climax":
                hat(start + step * STEP, 0.6 + 0.4 * (step % 2 == 0), step in (6, 14))
            elif step % 2 == 0:
                hat(start + step * STEP, 0.4 + 0.6 * (step % 4 == 2), step == 14)
    elif part == "break" and bar >= 44:
        for step in range(0, 16, 4):
            kick(start + step * STEP, 0.6 + 0.1 * (bar - 44))
    # 主旋律：主题段一遍、高潮段两遍并叠高八度
    if part in ("theme", "climax"):
        local = (bar - 24) % 8
        if part == "climax":
            local = (bar - 48) % 8
        for melody_bar, step, length, midi in MELODY:
            if melody_bar != local:
                continue
            lead(start + step * STEP, midi, length * STEP * 0.95, 1.0)
            if part == "climax":
                lead(start + step * STEP, midi + 12, length * STEP * 0.95, 0.35)

def fft_convolve(signal, ir):
    # 频域卷积，结果长度为两者之和
    size = 1 << int(np.ceil(np.log2(len(signal) + len(ir))))
    return np.fft.irfft(np.fft.rfft(signal, size) * np.fft.rfft(ir, size), size)[:len(signal) + len(ir) - 1]


def wrap(signal):
    # 把超出曲长的尾音折回开头，保证循环无缝
    out = signal[:LENGTH].copy()
    tail = signal[LENGTH:]
    out[:len(tail)] += tail
    return out


# 乒乓延迟：附点八分音符，左右交替反馈
delay_samples = int(STEP * 3.0 * SR)
delayed = np.zeros((2, LENGTH + delay_samples * 8))
for tap in range(1, 7):
    gain = 0.45 ** tap
    channel = tap % 2
    source = arp_bus[1 - channel]
    delayed[channel, delay_samples * tap:delay_samples * tap + LENGTH] += source * gain
delayed = np.stack([wrap(delayed[0]), wrap(delayed[1])])
send += delayed * 0.4

# 混响：左右声道独立的指数衰减噪声脉冲响应
ir_length = int(2.6 * SR)
ir_time = np.arange(ir_length) / SR
reverb = np.zeros((2, LENGTH))
for channel in range(2):
    ir = RNG.standard_normal(ir_length) * np.exp(-ir_time / 0.55)
    ir = np.convolve(ir, np.ones(8) / 8, mode="same")
    ir[:int(0.02 * SR)] = 0.0
    ir /= np.sqrt(np.sum(ir ** 2))
    reverb[channel] = wrap(fft_convolve(send[channel], ir))

mix = dry + delayed + reverb * 0.9
# 母线软限幅并归一化到 -1dBFS
mix -= mix.mean(axis=1, keepdims=True)
mix = np.tanh(mix / np.max(np.abs(mix)) * 1.8)
mix *= 0.89 / np.max(np.abs(mix))
# 首尾 20 毫秒淡入淡出，避免重播时爆音
fade = int(0.02 * SR)
mix[:, :fade] *= np.linspace(0.0, 1.0, fade)
mix[:, -fade:] *= np.linspace(1.0, 0.0, fade)

wav_path = os.path.join(HERE, "flying_saucer_boss.wav")
with wave.open(wav_path, "wb") as handle:
    handle.setnchannels(2)
    handle.setsampwidth(2)
    handle.setframerate(SR)
    handle.writeframes((mix.T * 32767.0).astype("<i2").tobytes())

os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "5", OUTPUT], check=True)
print("written", os.path.abspath(OUTPUT), "seconds", LENGTH / SR)
