# Person 4 — integration summary

Data sources — validation: file; test: file

| Approach | Threshold | Mean delay (s) | P95 delay (s) | Correct early prep. (%) | Unused capacity (cap-s) | Handovers | Correct | Wrong | Expired |
|---|---|---|---|---|---|---|---|---|---|
| Reactive | — | 2.000 | 2.00 | 0.0 | 0 | 176 | 0 | 0 | 0 |
| Movement rule | — | 0.648 | 2.00 | 75.0 | 790 | 176 | 132 | 2 | 158 |
| Pre-trained GRU | 0.60 | 0.597 | 2.00 | 79.0 | 210 | 176 | 139 | 2 | 42 |
| Scratch GRU (10 trips) | 0.99 | 2.000 | 2.00 | 0.0 | 0 | 176 | 0 | 0 | 0 |
| Scratch GRU (25 trips) | 0.99 | 2.000 | 2.00 | 0.0 | 0 | 176 | 0 | 0 | 0 |
| Scratch GRU (50 trips) | 0.50 | 1.540 | 2.00 | 28.4 | 185 | 176 | 50 | 0 | 37 |
| Fine-tuned GRU (10 trips) | 0.55 | 0.625 | 2.00 | 76.7 | 200 | 176 | 135 | 1 | 40 |
| Fine-tuned GRU (25 trips) | 0.55 | 0.761 | 2.00 | 71.6 | 220 | 176 | 126 | 1 | 44 |
| Fine-tuned GRU (50 trips) | 0.50 | 0.739 | 2.00 | 72.2 | 180 | 176 | 127 | 1 | 36 |

## Findings

**Scope.** 9 of 9 approaches evaluated on 100 held-out trips with 176 handovers each (all approaches see the same handovers).
**Baselines.** Reactive: mean delay 2.000 s, P95 2.00 s, correct early rate 0.0%, unused capacity 0. Movement-rule: mean delay 0.648 s, P95 2.00 s, correct early rate 75.0%, unused capacity 790 capacity-seconds (2 wrong reservations).
**Fine-tuning vs scratch training.**
- 10 trips: fine-tuned vs scratch — mean delay 0.625 vs 2.000 s (better), P95 2.00 vs 2.00 s (tied), correct early 76.7 vs 0.0% (better), unused capacity 200 vs 0 (worse); thresholds 0.55 vs 0.99.
- 25 trips: fine-tuned vs scratch — mean delay 0.761 vs 2.000 s (better), P95 2.00 vs 2.00 s (tied), correct early 71.6 vs 0.0% (better), unused capacity 220 vs 0 (worse); thresholds 0.55 vs 0.99.
- 50 trips: fine-tuned vs scratch — mean delay 0.739 vs 1.540 s (better), P95 2.00 vs 2.00 s (tied), correct early 72.2 vs 28.4% (better), unused capacity 180 vs 185 (better); thresholds 0.50 vs 0.50.
  Across the 3 budget(s) compared, fine-tuned was better than scratch on mean delay in 3, P95 delay in 0, correct early rate in 3 and unused capacity in 1.
  Mean-delay change from adapting the pre-trained model (pre-trained minus fine-tuned; positive = fine-tuning helped): 10: -0.028 s; 25: -0.165 s; 50: -0.142 s.
  Fine-tuned mean delay by adaptation trips: 10→0.625 s, 25→0.761 s, 50→0.739 s.
  Scratch mean delay by adaptation trips: 10→2.000 s, 25→2.000 s, 50→1.540 s.
**Delay vs unused capacity.**
- Movement rule: mean delay -1.352 s vs reactive, unused capacity 790 capacity-s, i.e. 3.32 capacity-s per second of total delay saved.
- Pre-trained GRU: mean delay -1.403 s vs reactive, unused capacity 210 capacity-s, i.e. 0.85 capacity-s per second of total delay saved.
- Scratch GRU (10 trips): mean delay +0.000 s vs reactive, unused capacity 0 capacity-s, no delay saved relative to reactive.
- Scratch GRU (25 trips): mean delay +0.000 s vs reactive, unused capacity 0 capacity-s, no delay saved relative to reactive.
- Scratch GRU (50 trips): mean delay -0.460 s vs reactive, unused capacity 185 capacity-s, i.e. 2.28 capacity-s per second of total delay saved.
- Fine-tuned GRU (10 trips): mean delay -1.375 s vs reactive, unused capacity 200 capacity-s, i.e. 0.83 capacity-s per second of total delay saved.
- Fine-tuned GRU (25 trips): mean delay -1.239 s vs reactive, unused capacity 220 capacity-s, i.e. 1.01 capacity-s per second of total delay saved.
- Fine-tuned GRU (50 trips): mean delay -1.261 s vs reactive, unused capacity 180 capacity-s, i.e. 0.81 capacity-s per second of total delay saved.
  Not dominated on (mean delay, unused capacity) — i.e. on the efficient frontier: Reactive, Pre-trained GRU, Scratch GRU (10 trips), Scratch GRU (25 trips), Fine-tuned GRU (10 trips), Fine-tuned GRU (50 trips).
  Spearman rank correlation between mean delay and unused capacity across the 9 evaluated approaches: -0.78 (negative: higher unused capacity goes with lower delay; positive: the opposite; descriptive only, not causal).
**Caveats.** One simulated scenario, one trained model per configuration (training seed fixed by Person 1), and 100 test trips on a shared clock with capacity 10 per cell; no confidence intervals were computed, so small differences should not be over-interpreted. Results describe this simulation and its assumptions only.