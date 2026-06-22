# Design — meinchat Cover Channel (`cover_v1`)

**Status:** Design / RFC (no code yet)
**Date:** 2026-06-22
**Scope decided with requester:** linguistic (text) cover · provably-secure scheme (Meteor/Discop) · Android client first
**Owner module:** `:plugins:meinchat-plus` (extends `:plugins:meinchat` through the existing `MeinChatSecureMessaging` seam — **`:core` is not touched**)

---

## 1. Goal

Make a private message **unobservable**, not merely confidential. After this feature,
a stored or transmitted message must look like *ordinary chat* — a plausible human
sentence — to anyone inspecting the database, Redis, or the network. The real payload
is recovered only by the intended recipient's client.

> Bob types `I want to buy you an Apple`.
> What lands in the DB / Redis / on the wire is e.g.
> `within the other part of the evening i will find the rest of the order` —
> a grammatical sentence carrying no visible secret.
> Alice's client recovers `I want to buy you an Apple`.

This is **not** a replacement for encryption. It is a layer *on top of* encryption that
defeats traffic analysis and pattern detection ("this field looks encrypted / high-entropy
/ suspicious").

## 2. Threat model (be precise — it dictates everything)

| # | Adversary | Capability | What defeats it |
|---|-----------|-----------|-----------------|
| A | **DB / Redis admin (insider)** | Reads stored rows at rest | E2E already hides *content*; cover hides that a secret *exists* |
| B | **Network monitor / DPI** | Sees traffic, flags "encrypted communication patterns" | Cover makes the channel look like normal chat |
| C | **Active steganalyst** | Runs statistical / ML steganalysis (SRNet-class detectors, distribution tests) | **Only provably-secure stego** (§5) survives this |
| D | **Endpoint compromise** | Owns a device | Out of scope — nothing in transit/at rest helps here |

Chosen target: **A + B + C.** That is why the scheme is Meteor/Discop-class, not a
word-dictionary mimic (which falls to C immediately — see §5 grading).

**Non-goals (v1):** hiding **metadata** — message length, send timing, conversation
frequency, who-talks-to-whom. Cover text hides *content*, not *shape*. See §9 — this is
the largest residual leak and is explicitly deferred.

## 3. The non-negotiable ordering

```
SEND
  plaintext
    ── AEAD encrypt (existing e2e_v1) ─────────▶ envelope bytes   (indistinguishable from random)
    ── cover-encode (Meteor/Discop) ───────────▶ cover sentence   (indistinguishable from model text)
    ── post as an ordinary message body ───────▶ server / DB / Redis

RECV
  cover sentence
    ── cover-decode ───────────────────────────▶ envelope bytes
    ── AEAD decrypt (existing e2e_v1) ──────────▶ plaintext
```

Two invariants, both load-bearing:

1. **Encrypt first, cover second.** The cover step is *not* confidentiality. Its security
   proof (§5) *requires* its input to be uniform random bits — which is exactly what an
   AEAD ciphertext/envelope is. Feeding plaintext (or compressible data) into the
   steganographic sampler breaks the indistinguishability guarantee. The existing
   `e2e_v1` envelope is the correct, ready-made input.
2. **The cover step is reversible and keyless-for-shape.** Decoding the cover yields the
   envelope; only the AEAD key (already managed by `meinchat-plus`) yields plaintext.

## 4. Why this is small in *our* codebase

The hard half already exists. From `meinchat/domain/`:

- `MeinChatSecureMessaging` — the seam `meinchat-plus` implements; `sendSecure()` /
  `decryptIncoming()` already do the AEAD + envelope work and enforce **fail-closed**
  (never transmit plaintext for a secure row).
- `ChatMessage.envelopeB64` / `protocol = "e2e_v1"` / `senderDeviceId` — the existing
  secure wire shape.

The cover channel is a **new transform inserted between the envelope and the body**, plus
a new seam (`CoverCodec`) it delegates to. It lives entirely in `meinchat-plus`, which
already declares its peer edge to `meinchat` (`PluginMetadata.dependencies = ["meinchat"]`
+ `declaredPeerDependencies`). **No `:core` change, no new module, no boundary change.**

## 5. The scheme — Meteor / Discop (provably-secure linguistic stego)

### Primer
A language model defines, at each step, a probability distribution over the next token.
Normal generation *samples* from it. **Meteor** (Kaptchuk, Jois, Green, Rubin — 2021,
*"Meteor: Cryptographically Secure Steganography for Realistic Distributions"*) instead
uses the **uniform random ciphertext bits to drive the sampling** via range/arithmetic
coding: the chosen token is the one whose probability interval the ciphertext bits fall
into. The output is a genuine sample from the model's distribution, so to anyone without
the key it is **computationally indistinguishable from ordinary model text** — this is a
provable property, not "looks fine to me." **Discop** (Ding et al. — 2023,
*"Provably Secure Steganography... Distribution Copies"*) keeps the same security and
improves **capacity** and is **more robust to cross-device numerical drift** because it
codes by *rank* rather than raw float intervals (see §7 risk).

### Why not the cheap option
| Technique | Fools human/admin (A) | Fools DPI (B) | Survives steganalysis (C) | Capacity | On-device cost |
|---|---|---|---|---|---|
| Word-dictionary / grammar mimic (spammimic) | ✅ | ✅ | ❌ token stats betray it | low | trivial |
| **Meteor / Discop (chosen)** | ✅ | ✅ | ✅ by construction | ~1–4 bits/token | **runs an LM on-device** |

The dictionary mimic is a good *demo* and a good *fallback codec*, but it does not meet
target C. We implement the seam so both can coexist (§6), and ship Meteor/Discop as the
default.

### Capacity → message length (a real consequence)
At ~1–4 bits/token, a typical ~120-byte envelope (≈960 bits) expands to **~240–960 tokens**
of cover — i.e. a long paragraph, not a one-liner. Implications:
- The cover sentence is **much longer** than the secret. That is inherent to safe
  linguistic stego. UX must accept that the visible "decoy" text is verbose.
- Length is itself a metadata signal (§9). Consider compressing plaintext *before*
  encrypt, and/or fixing envelope size by padding so all cover messages are similar length.

## 6. The seam (`CoverCodec`) — contract

New interface in `meinchat-plus` (sketch — final names TBD in the sprint):

```kotlin
/** Reversible cover transform: ciphertext bytes <-> plausible cover text.
 *  NOT confidentiality — see design §3. Pluggable so a cheap fallback and the
 *  provably-secure default can coexist; selection is negotiated at pairing time. */
interface CoverCodec {
    val id: String                                   // e.g. "meteor_gpt2_124m_v1"

    /** envelope bytes -> cover sentence(s). MUST accept arbitrary (random) bytes. */
    suspend fun encode(payload: ByteArray, ctx: CoverContext): String

    /** cover sentence(s) -> envelope bytes, or null if this is not our cover. */
    suspend fun decode(cover: String, ctx: CoverContext): ByteArray?
}

/** Shared, deterministic decoding context both peers reproduce identically. */
data class CoverContext(
    val modelId: String,        // exact model build both sides run
    val seed: ByteArray,        // per-conversation, from the pairing handshake
    val tokenBudget: Int,
)
```

- A plugin/consumer depends on **this interface only** — matches the `PlatformSdk`
  seam philosophy (OCP/ISP/DIP). `meinchat-plus` wires its `MeinChatSecureMessaging`
  impl to call `cipher → CoverCodec.encode` on send and `CoverCodec.decode → cipher` on
  read.
- **Liskov / disabled-path:** if no `CoverCodec` is available or `decode` returns null,
  the conversation degrades to the existing `e2e_v1` (`envelope_b64`) path. A cover-mode
  peer talking to a non-cover peer must fail *closed*, never leak plaintext — same rule
  the secure seam already enforces.

## 7. Wire format & the "no marker" decision (important)

Naive approach: add `protocol = "cover_v1"` to the row. **Rejected** — that field value is
itself a tell to threat A (the DB admin sees a column literally labelled "this is hidden").

**Chosen approach — indistinguishable rows:** a cover conversation is provisioned as an
*ordinary* conversation. On the wire and at rest:
- `body` = the cover sentence (looks like a normal message).
- `protocol` = `null`, `envelope_b64` = `null` — **identical to a plain chat row.**
- The fact "this conversation is a cover channel, using codec X with seed S" exists
  **only in client-side state**, established during the `meinchat-plus` pairing handshake
  (alongside the existing device-key exchange). The server stores no copy.

Recipient logic: for a conversation the client *knows* is cover-mode, run
`CoverCodec.decode` on every inbound `body`; on null/garbage, fall back to showing the raw
body (covers an accidental plain message). Cost: a decode attempt per inbound message —
acceptable, and bounded by `tokenBudget`.

Trade-off accepted: you **cannot** freely mix plain and cover messages in the *same*
conversation without an in-band marker, which we are deliberately not adding. Cover-mode
is a per-conversation property fixed at pairing.

`MessageCache` (local, `domain/MessageCache.kt`): cache the **decoded plaintext** locally
under the device's existing at-rest protection, never the cover form — re-decoding on
every render is wasteful and the local store is already trusted (threat D is out of scope).

## 8. The shared model — the operational cost to own up to

Meteor/Discop require **both clients to run the same model, deterministically.**

- **What ships in the APK:** a small LM (e.g. GPT-2 124M-class, or a distilled/quantized
  model) executed on-device via ONNX Runtime / llama.cpp / MediaPipe LLM. This is real
  binary size (tens–hundreds of MB) and real inference latency per message (hundreds of ms
  to seconds on mid-tier devices). **This is the dominant cost of the whole feature** —
  flag it before committing.
- **Model = public, key = secret.** Security does **not** rest on the model being secret
  (it ships in the APK; assume the adversary has it). It rests on the AEAD key and on the
  indistinguishability proof. The `seed` in `CoverContext` only synchronises the two
  decoders; it is not the security boundary.
- **Versioning:** `modelId` must pin an exact build. A model update is a breaking change to
  the cover format and must be negotiated, not silently rolled out.

### 🔴 Top implementation risk — cross-device numerical determinism
Meteor's arithmetic coding needs the model's probability outputs to be **bit-identical** on
sender and receiver. Floating-point math differs across CPUs/GPUs/NNAPI backends → decode
fails. Mitigations, in order of preference:
1. Use **Discop's rank-based coding** (robust to small prob perturbations) rather than raw
   float intervals.
2. Force **integer/fixed-point** logit quantisation and a single pinned inference backend
   (CPU, no NNAPI) for the stego path.
3. Truncate to top-k and code over **ranks**, never raw probabilities.

Prove this end-to-end on two *different* physical devices **before** building UI. It is the
make-or-break unknown.

## 9. Residual leaks (metadata) — deferred, but stated

Cover text hides content, not shape. A monitor (B/C) can still see:
- **Length** — cover paragraphs are long and roughly proportional to payload size.
  *Mitigation (later):* compress-then-pad envelopes to fixed buckets.
- **Timing & cadence** — real-time replies at chat speed.
  *Mitigation (later):* jitter / scheduled flushing.
- **Social graph** — who talks to whom, how often.
  *Mitigation (later):* decoy/cover traffic — expensive, separate effort.

These are **out of scope for v1** and must be documented as known gaps so the feature is
not oversold as "undetectable." It is *content-unobservable*, not *traffic-unobservable*.

## 10. TDD plan (Red list — first failing tests)

Per the engineering rules (TDD-first, JUnit5 + MockK + Turbine + coroutines-test):

1. `CoverCodec` round-trip: `decode(encode(random bytes)) == bytes` for many random
   envelopes incl. empty / max-budget.
2. Output-is-text: `encode` output matches a natural-language shape (token set, length
   within `tokenBudget`).
3. **Determinism across instances:** two independently-constructed codecs with the same
   `CoverContext` produce identical encode output and decode each other (the §8 risk, as a
   test).
4. `decode` of arbitrary human text (not our cover) returns `null` — no false positives.
5. Wrong-seed / wrong-`modelId` → `decode` returns null (fail-closed, no plaintext leak).
6. Integration through `MeinChatSecureMessaging`: cover conversation send→read recovers
   plaintext; a stored row exposes `protocol=null, envelope_b64=null, body=<sentence>`
   (the §7 indistinguishability assertion).
7. Liskov: cover-peer ↔ non-cover-peer fails closed, never posts plaintext.
8. `dependencyBoundaryCheck` stays green (no new edges).

## 11. Phasing

- **P0 — spike (no UI):** prove §8 cross-device determinism with a chosen model + Discop
  ranks. Go/no-go gate. If determinism can't be made robust, fall back to the dictionary
  codec (meets A+B only) and re-scope.
- **P1 — `CoverCodec` seam + Meteor/Discop impl** behind it, full Red list §10, fallback
  dictionary codec for parity/tests.
- **P2 — wire into `meinchat-plus`** secure-messaging path + pairing-time negotiation of
  `CoverContext`; the "no marker" row format §7.
- **P3 — UX:** verbose-cover handling, decode-on-read, local plaintext cache.
- **P4 (later) — metadata hardening §9** (padding/jitter), if the threat model demands.

## 12. Open decisions

1. **Model choice & footprint** — GPT-2 124M (proven in the Meteor paper) vs a smaller
   distilled/quantised model. Trades APK size/latency against cover quality.
2. **Meteor vs Discop** — Discop preferred for capacity + determinism robustness; confirm
   against an available Kotlin/ONNX implementation or budget a port.
3. **Inference runtime** — ONNX Runtime Mobile vs llama.cpp (JNI) vs MediaPipe LLM; must
   support a deterministic CPU path (§8).
4. **Compression before encrypt** — shrinks cover length (§5/§9) but must not reintroduce
   structure into the AEAD input — encrypt still comes last, so this is safe; confirm.
5. **Cover topic control** — should the model be prompted so the decoy text stays
   on-topic/innocuous for a given user, or is generic model text enough?

---

*Next step after review: turn P0 into an A-series sprint doc and start the determinism
spike (the one experiment that decides whether the provably-secure path is viable on
real devices).*
