# Proposed Plan — AI-Driven Mobility Prediction in 6G

## Problem statement

When a mobile user moves between cells, the target base station needs resources ready to serve the user. Starting preparation at the handover trigger can leave the user waiting for that setup. Preparing too early or at the wrong cell ties up capacity. This semester project will simulate both effects on a campus network.

**Research question.** Can a mobility model fine-tuned on a small number of campus trips reduce the preparation delay remaining at handover, while keeping unused reservations within a comparable capacity budget?

## 1. Generate a campus mobility dataset

Create a 2D map with pedestrian paths connecting a hostel, library, lecture halls, and canteen. Place four base stations on the map and simulate users taking different routes, walking at different speeds, and stopping along the way. Record model inputs once per simulated second. Assign the serving cell by the closest-base-station rule and timestamp handovers at the coverage boundary crossings. Use a finer event clock for crossings and resource preparation so the model’s sampling interval does not round away short delays.

| Dataset field | Purpose |
|---|---|
| `trip_id`, `time_s` | Keep readings in order and split by complete trip |
| `x_m`, `y_m`, `speed_m_s`, `heading_rad` | Describe where the user is and how they are moving |
| `serving_cell` | Identify the current cell and derive future training labels |

Generate **general campus trips** for pre-training and a **morning class-rush scenario** for adaptation, where trips from the hostel favor lecture halls. Keep the map and cell IDs fixed across both scenarios. Reserve separate morning trips for fine-tuning, validation, and final testing before creating training windows. Save the simulation seeds so runs are reproducible.

## 2. Train and fine-tune the predictor

Use a small GRU, a neural network that reads sequences. Its input is the **last five readings**. Its target is the **serving cell three seconds ahead**, which can be the current cell. The output is a probability for each cell. For example, readings from seconds 8–12 predict the cell at second 15.

1. Pre-train the GRU on diverse routes from the general campus dataset.
2. Continue training the same weights on morning trips. Try adaptation sets of **10, 25, and 50 trips**.
3. Train a second GRU from scratch on each of those same small adaptation sets to test whether pre-training helps when campus-specific data is limited.
4. Use validation trips to select the training checkpoint and reservation confidence threshold. Keep the final test trips for the frozen comparison.

## 3. Simulate proactive handover preparation

At each reading, the policy checks whether the most likely cell differs from the current serving cell, its probability clears the threshold, and the target has capacity available. If all conditions hold, it reserves one simulated capacity unit and starts preparation. A user can have one pending reservation at a time. A matching handover uses the reservation. Unused reservations expire and release capacity.

Replay identical test trips, cell-change triggers, and capacity settings across these approaches:

| Approach | How it prepares |
|---|---|
| Reactive baseline | Starts preparation when the actual handover triggers |
| Movement-rule baseline | Projects current speed and direction three seconds ahead, then estimates the target cell |
| Pre-trained GRU | Predicts using the general campus training |
| Morning-only GRU | Predicts using a model trained from scratch on the small morning dataset |
| Fine-tuned GRU | Predicts using the general model adapted to morning trips |

All proactive approaches use the same capacity and reservation lifetime rules. Compare them at similar reservation costs to assess the effect of prediction quality.

## 4. Evaluate the results

- **Target-cell prediction accuracy.** Among prediction windows where the serving cell changes by the forecast horizon, measure how often the model predicts the correct future cell.
- **Correct early preparation rate.** Percentage of actual handovers with an active advance reservation at the correct target cell when handover triggers.
- **Remaining preparation delay.** Mean and 95th-percentile simulated wait for preparation after the trigger. For example, if setup takes 50 ms and began 30 ms before the trigger, 20 ms remains.
- **Unused capacity.** Capacity units multiplied by how long they were held for reservations that expired unused. Report this alongside delay so a more aggressive policy has a visible cost.

The experiment uses simulated movement, coverage, and setup times. Its results describe this campus scenario and its assumptions.

## Planned files

| File | Responsibility |
|---|---|
| `train_model.ipynb` | Generate and verify campus trips, create training windows, pre-train, fine-tune, and evaluate the GRU |
| `handover_simulator.ipynb` (planned) | Apply reservation policies and measure delay and capacity usage |
| `main.ipynb` (planned) | Run reproducible experiments and present the results |

## Proposed semester milestones

| Weeks | Milestone |
|---|---|
| 1–2 | Build the map and verify trip generation and cell changes |
| 3–4 | Implement the reactive and movement-rule baselines |
| 5–7 | Train the GRU and run the fine-tuning experiments |
| 8–9 | Run held-out comparisons and analyze delay versus unused capacity |
| 10 | Prepare the demo and summarize the results, assumptions, and limitations |
