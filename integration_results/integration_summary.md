# Person 4 — integration summary

Data sources — validation: RECONSTRUCTED in memory from Person 1's generator + seed (data file missing; nothing written to data/); test: RECONSTRUCTED in memory from Person 1's generator + seed (data file missing; nothing written to data/)

| Approach | Threshold | Mean delay (s) | P95 delay (s) | Correct early prep. (%) | Unused capacity (cap-s) | Handovers | Correct | Wrong | Expired |
|---|---|---|---|---|---|---|---|---|---|
| Reactive | — | 2.000 | 2.00 | 0.0 | 0 | 176 | 0 | 0 | 0 |
| Movement rule | — | 0.648 | 2.00 | 75.0 | 790 | 176 | 132 | 2 | 158 |
| Pre-trained GRU | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING |
| Scratch GRU (10 trips) | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING |
| Scratch GRU (25 trips) | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING |
| Scratch GRU (50 trips) | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING |
| Fine-tuned GRU (10 trips) | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING |
| Fine-tuned GRU (25 trips) | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING |
| Fine-tuned GRU (50 trips) | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING | PENDING |

## Findings

**Scope.** 2 of 9 approaches evaluated on 100 held-out trips with 176 handovers each (all approaches see the same handovers).
**Pending (not evaluated, no values invented):** Pre-trained GRU, Scratch GRU (10 trips), Scratch GRU (25 trips), Scratch GRU (50 trips), Fine-tuned GRU (10 trips), Fine-tuned GRU (25 trips), Fine-tuned GRU (50 trips).
**Baselines.** Reactive: mean delay 2.000 s, P95 2.00 s, correct early rate 0.0%, unused capacity 0. Movement-rule: mean delay 0.648 s, P95 2.00 s, correct early rate 75.0%, unused capacity 790 capacity-seconds (2 wrong reservations).
**Fine-tuning vs scratch training.**
- 10 trips: PENDING (Fine-tuned GRU (10 trips), Scratch GRU (10 trips) not evaluated), so no comparison can be made.
- 25 trips: PENDING (Fine-tuned GRU (25 trips), Scratch GRU (25 trips) not evaluated), so no comparison can be made.
- 50 trips: PENDING (Fine-tuned GRU (50 trips), Scratch GRU (50 trips) not evaluated), so no comparison can be made.
**Delay vs unused capacity.**
- Movement rule: mean delay -1.352 s vs reactive, unused capacity 790 capacity-s, i.e. 3.32 capacity-s per second of total delay saved.
  Not dominated on (mean delay, unused capacity) — i.e. on the efficient frontier: Reactive, Movement rule.
**Caveats.** One simulated scenario, one trained model per configuration (training seed fixed by Person 1), and 100 test trips on a shared clock with capacity 10 per cell; no confidence intervals were computed, so small differences should not be over-interpreted. Results describe this simulation and its assumptions only.