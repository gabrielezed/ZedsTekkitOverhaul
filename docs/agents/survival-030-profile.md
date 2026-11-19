# Alpha.3 settlement performance

The [extended acceptance record](survival-030-implementation.md#extended-alpha3-candidate) identifies the mod and agent. Run `zto-tools/test-zto-agent.ps1 -Focus settlement-profile` twice with that pair and the pinned pack. Each invocation uses a new Java 8u51 JVM and disposable worlds. The recorded runs used Windows 11 on a Ryzen 7 5800X. Measurements use nearest-rank percentiles and omit p99 below 100 samples. The deterministic checks gate work and correctness. Latency varies with JVM compilation, GC, scheduling and filesystem cache state.

## Local recognition and remote Quarry

Each local case warms the service for 3,000 ticks, then measures 36 cycles. The first slice of the first cycle aligns the capture. The native cabin has furnished low-roof geometry. The industrial workshop has dense planks, furnaces and pistons with a central working space. Its fixtures exercise geometry and material lookup with static machinery. World setup, movement calls, Territorial Activity updates and diagnostic snapshots sit outside the timer. Final ticks include the last slice, component/utility/cover evaluation and the registry decision. The component-only measurement invokes the production pass immediately after sampling an identical window, with 512 warm-up and 144 measured passes. It includes reflective invocation.

The entrance recentered on all 36 cycles. The movement route moved the observer beyond the anchor tolerance on all 36 cycles before classification. Every sampled update performed exactly 169 block reads. The remote control ran for 400 local updates with the remote terrain loaded and another 120 updates with no eligible observer. On the same terrain a preconfigured BuildCraft 2.2.14 Quarry arm used native `TileQuarry` ticks, received MJ and drove the native excavation callback. The fixture supplied the post-blueprint setup and targets. Both runs observed 12 removals in 306 powered ticks, consuming 1,248 MJ. The local territory and recognition updates made zero remote chunk lookups or requests for absent chunks in either condition, and the 120 no-observer updates performed zero survey reads. The callback microbenchmark measures agent dispatch, validation and the ledger update on already-mutated blocks, independently of the native write and machine work.

## Request pressure model

Overlapping origins all use the same dense workshop. Dispersed origins use identical geometry spaced 32 blocks apart. Each origin has its own recognition state and no corroborating activity, registry writes or pooled interaction evidence. The harness services 1, 8, 16 or 32 origins synchronously on one thread. It warms each case for 600 frames, audits chunk requests during warm-up and records 3,600 frames with auditing disabled. The separate geometry assertion checks 1,266 inferred blocks at every completed origin. A 32-origin sampled frame performs 5,408 block reads by design. The model excludes the actual multiplayer server, packet handling and concurrent game systems.

## Provenance at capacity

The profile fills 1,024 sections with either one or 512 observations per section. The compressed files contain 1,820 and 250,204 bytes respectively for these deterministic inputs. Each of 24 measured rounds after four warm-up rounds changes one existing record per section. The update metric invokes the real store method through reflection. For decomposition, the same production codec writes gzip into memory, and a separate operation writes the matching bytes to a temporary file and atomically replaces a destination. The real dirty save streams through its usual gzip file path and is checked byte-for-byte against the in-memory result. Clean save and ledger reopen are independently timed. File replacement timings reflect the native API and the host's filesystem cache. They carry no durable-write guarantee.

The dense dirty save took 228–252 ms at the median versus 3.5–4.0 ms for the sparse store. Dense gzip to memory took 219–243 ms at the median and replaying the filesystem write with already-compressed bytes took about 1.8 ms. Dense reopen took about 194 ms. Both 32-origin pressure models stayed below 0.82 ms in the observed final frames. Codec work is the first performance cost worth investigating for a future server with busy, densely edited worlds.

## Dedicated server handoff

Give each world a shared budget for loaded block reads and completed component evaluations per server tick. Stagger admissions and use a bounded round-robin queue with age or deadline checks to keep distant and recently invalidated requests from starving. Charge the same budget for recentered windows and coalesce spatial sampling of overlapping windows where their chunk identities and data revisions agree. Keep walking, openings and qualifying activity scoped to the observer that supplied them.

The world thread should own live chunk access, observation and registry updates. A pure component pass could consume immutable sampled flags if a measured server bottleneck survives staggering and deduplication. This profile gives higher priority to investigating a buffered codec and bounded background compression of immutable **provenance** snapshots. Preserve one authoritative dirty revision. At completion, compare the save/session identity and revision so an older result cannot overwrite a newer file or clear a newer dirty revision.

For any future queued survey result, carry the world/session identity, origin and latest request generation, chunk residency identity and sampled-data revisions. Recheck those tokens, observer eligibility and current corroborating activity on the world thread before applying it to the registry. Discard results from superseded requests, changed slices or unloaded/replaced chunks, and resubmit through the ordinary budget. Real dedicated-server throughput and compatibility remain unverified.

## Timing samples

The values in each run column are **median / p95 / p99 / max**, in microseconds. Sample counts are per run.

### Local and remote workloads

| Workload | Samples | Run 1 | Run 2 |
| --- | ---: | --- | --- |
| cabin center idle | 3276 | 0.100 / 0.200 / 0.300 / 13.300 | 0.100 / 0.200 / 0.300 / 14.500 |
| cabin center initial slice | 287 | 4.700 / 9.000 / 40.200 / 103.600 | 4.600 / 9.900 / 34.400 / 132.500 |
| cabin center initial final | 36 | 72.200 / 102.600 / - / 104.600 | 69.500 / 101.200 / - / 107.200 |
| cabin entrance idle | 2952 | 0.100 / 0.200 / 0.200 / 4.100 | 0.100 / 0.200 / 0.200 / 4.500 |
| cabin entrance initial slice | 287 | 3.400 / 5.000 / 5.800 / 9.600 | 2.500 / 4.500 / 6.200 / 8.500 |
| cabin entrance initial final | 36 | 18.300 / 23.200 / - / 26.700 | 13.500 / 22.300 / - / 29.200 |
| cabin entrance recentered slice | 288 | 2.700 / 4.500 / 5.700 / 9.200 | 2.400 / 4.100 / 5.100 / 8.600 |
| cabin entrance recentered final | 36 | 19.400 / 27.400 / - / 29.100 | 14.600 / 28.200 / - / 28.500 |
| cabin movement idle | 3348 | 0.100 / 0.100 / 0.200 / 3.800 | 0.100 / 0.200 / 0.300 / 7.100 |
| cabin movement initial slice | 215 | 2.600 / 16.100 / 20.000 / 46.700 | 2.600 / 4.000 / 6.600 / 27.600 |
| cabin movement cancellation | 36 | 0.100 / 1.600 / - / 4.500 | 0.100 / 0.200 / - / 9.600 |
| cabin center component pass alone | 144 | 6.400 / 7.800 / 38.000 / 63.000 | 6.300 / 6.400 / 32.600 / 38.800 |
| dense industrial workshop idle | 3276 | 0.100 / 0.100 / 0.200 / 0.400 | 0.100 / 0.200 / 0.200 / 5.400 |
| dense industrial workshop initial slice | 287 | 5.900 / 9.400 / 15.900 / 19.000 | 6.200 / 10.900 / 15.800 / 18.900 |
| dense industrial workshop initial final | 36 | 48.100 / 61.000 / - / 64.400 | 39.400 / 73.100 / - / 73.300 |
| dense industrial workshop component pass alone | 144 | 12.100 / 17.500 / 23.300 / 23.900 | 11.700 / 11.900 / 14.400 / 29.200 |
| loaded remote terrain / local idle | 364 | 0.100 / 0.100 / 0.200 / 2.800 | 0.000 / 0.100 / 0.100 / 2.100 |
| loaded remote terrain / local sampled | 36 | 13.100 / 47.400 / - / 134.600 | 9.100 / 51.100 / - / 84.900 |
| powered remote quarry / local idle | 273 | 0.100 / 0.100 / 0.300 / 0.400 | 0.100 / 0.100 / 0.300 / 0.300 |
| powered remote quarry / local sampled | 33 | 6.800 / 37.500 / - / 39.000 | 6.900 / 33.400 / - / 36.200 |
| callback placement | 3600 | 0.100 / 0.300 / 1.100 / 18.300 | 0.100 / 0.600 / 1.300 / 25.100 |
| callback manual removal | 3600 | 0.100 / 0.300 / 1.100 / 10.300 | 0.100 / 0.500 / 1.000 / 27.200 |
| callback remote quarry removal | 3600 | 0.100 / 0.300 / 0.500 / 21.300 | 0.100 / 0.300 / 0.500 / 21.100 |

### Scaling model

| Workload | Samples | Run 1 | Run 2 |
| --- | ---: | --- | --- |
| overlapping 1 idle frame | 3276 | 0.100 / 0.100 / 0.100 / 0.500 | 0.100 / 0.100 / 0.100 / 4.500 |
| overlapping 1 slice frame | 288 | 3.600 / 3.800 / 6.100 / 11.100 | 3.500 / 3.700 / 4.200 / 4.500 |
| overlapping 1 final frame | 36 | 15.200 / 15.400 / - / 15.900 | 14.700 / 15.200 / - / 18.500 |
| overlapping 8 idle frame | 3276 | 0.300 / 0.400 / 0.500 / 8.600 | 0.300 / 0.400 / 0.500 / 6.500 |
| overlapping 8 slice frame | 288 | 28.000 / 29.500 / 34.200 / 35.200 | 27.200 / 28.200 / 30.300 / 31.200 |
| overlapping 8 final frame | 36 | 122.300 / 138.500 / - / 142.700 | 117.200 / 129.600 / - / 132.100 |
| overlapping 16 idle frame | 3276 | 0.200 / 0.300 / 0.300 / 1.200 | 0.200 / 0.200 / 0.300 / 1.100 |
| overlapping 16 slice frame | 288 | 54.800 / 56.800 / 62.600 / 65.600 | 54.500 / 56.000 / 59.200 / 62.400 |
| overlapping 16 final frame | 36 | 241.400 / 248.400 / - / 254.900 | 234.500 / 248.600 / - / 268.600 |
| overlapping 32 idle frame | 3276 | 0.400 / 0.500 / 0.600 / 1.200 | 0.500 / 0.700 / 0.800 / 4.000 |
| overlapping 32 slice frame | 288 | 109.800 / 113.200 / 123.400 / 3967.900 | 110.600 / 171.700 / 173.700 / 181.300 |
| overlapping 32 final frame | 36 | 478.100 / 510.400 / - / 510.600 | 475.800 / 809.000 / - / 816.200 |
| dispersed 1 idle frame | 3276 | 0.000 / 0.100 / 0.100 / 0.100 | 0.000 / 0.100 / 0.100 / 3.500 |
| dispersed 1 slice frame | 288 | 3.400 / 3.600 / 3.800 / 4.000 | 3.500 / 4.400 / 4.500 / 4.600 |
| dispersed 1 final frame | 36 | 15.000 / 15.100 / - / 17.700 | 14.700 / 17.900 / - / 20.400 |
| dispersed 8 idle frame | 3276 | 0.100 / 0.200 / 0.300 / 25.000 | 0.100 / 0.200 / 0.300 / 17.100 |
| dispersed 8 slice frame | 288 | 21.600 / 25.600 / 27.900 / 30.500 | 21.500 / 26.600 / 30.300 / 33.300 |
| dispersed 8 final frame | 36 | 113.700 / 132.300 / - / 136.900 | 110.500 / 139.300 / - / 155.300 |
| dispersed 16 idle frame | 3276 | 0.200 / 0.300 / 0.500 / 3.400 | 0.200 / 0.300 / 0.300 / 2.300 |
| dispersed 16 slice frame | 288 | 39.300 / 45.200 / 65.000 / 67.000 | 38.900 / 40.800 / 43.200 / 46.700 |
| dispersed 16 final frame | 36 | 227.100 / 247.300 / - / 265.600 | 219.200 / 223.100 / - / 223.100 |
| dispersed 32 idle frame | 3276 | 0.400 / 0.700 / 0.900 / 4.200 | 0.400 / 0.700 / 0.800 / 7.300 |
| dispersed 32 slice frame | 288 | 81.300 / 133.700 / 142.700 / 152.300 | 79.600 / 140.600 / 152.500 / 155.200 |
| dispersed 32 final frame | 36 | 462.700 / 756.200 / - / 785.900 | 442.600 / 806.000 / - / 810.700 |

### Storage

| Workload | Samples | Run 1 | Run 2 |
| --- | ---: | --- | --- |
| 1 tracked positions/section record | 24576 | 0.300 / 0.700 / 0.900 / 12.500 | 0.300 / 0.700 / 1.100 / 39.200 |
| 1 tracked positions/section gzip to memory | 24 | 1486.000 / 2555.800 / - / 2708.900 | 1511.200 / 2415.800 / - / 2957.200 |
| 1 tracked positions/section bytes + atomic replace | 24 | 1099.600 / 1333.600 / - / 1355.800 | 1260.600 / 1629.800 / - / 2666.100 |
| 1 tracked positions/section dirty save | 24 | 3535.300 / 4264.500 / - / 4388.800 | 3956.800 / 5290.700 / - / 5431.400 |
| 1 tracked positions/section clean save | 24 | 2.400 / 6.500 / - / 265.700 | 2.700 / 9.200 / - / 315.000 |
| 1 tracked positions/section reopen | 24 | 1531.900 / 2470.800 / - / 7649.900 | 1648.500 / 2421.100 / - / 8768.000 |
| 512 tracked positions/section record | 24576 | 0.100 / 0.300 / 0.700 / 14.400 | 0.100 / 0.400 / 0.700 / 13.600 |
| 512 tracked positions/section gzip to memory | 24 | 218849.100 / 245815.300 / - / 259293.300 | 243430.300 / 298115.600 / - / 303155.200 |
| 512 tracked positions/section bytes + atomic replace | 24 | 1832.700 / 1999.100 / - / 2133.700 | 1785.800 / 2019.900 / - / 2222.600 |
| 512 tracked positions/section dirty save | 24 | 228105.100 / 250880.000 / - / 259110.600 | 252073.700 / 285508.400 / - / 326013.300 |
| 512 tracked positions/section clean save | 24 | 2.200 / 3.200 / - / 13.300 | 2.200 / 3.400 / - / 13.400 |
| 512 tracked positions/section reopen | 24 | 193626.300 / 223479.400 / - / 225172.200 | 193320.500 / 230888.800 / - / 329682.200 |
