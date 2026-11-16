# Campus Mobility Dataset

Simulated pedestrian trips on a 400 x 400 m campus. Open `train_model.ipynb` and run its
dataset generation cells to create the CSV files in `data/`.

## The map

| Place | (x, y) m |      | Base station (cell ID) | (x, y) m |
|---|---|---|---|---|
| hostel | (50, 50) | | 0 | (100, 100) |
| library | (350, 80) | | 1 | (300, 100) |
| lecture_a | (230, 290) | | 2 | (100, 300) |
| lecture_b | (340, 330) | | 3 | (300, 300) |
| canteen | (60, 300) | | | |

Users walk between places at a constant speed (1.0 to 1.6 m/s per trip) and pause 0 to 20 s
at intermediate stops. Each leg goes through one random via-point (up to 40 m off the straight
line), so the same origin and destination can have different routes. The serving cell is the
closest base station to the true position.
A change in `serving_cell` between two consecutive rows is a handover.

## Columns

One row per simulated second, per trip.

| Column | Meaning | Where it is used |
|---|---|---|
| `trip_id` | Trip number, unique within one file | Split by whole trip (never mix rows of one trip across train/val/test). Windows must not cross trip boundaries |
| `time_s` | Seconds since the trip started (0, 1, 2, ...) | Keeps rows in order. Used to build windows and to place handovers in time |
| `x_m`, `y_m` | Position in metres, with 3 m Gaussian noise (like GPS). `serving_cell` uses the true position, so near a cell boundary the noisy point can look like it is in the neighbouring cell | Model input. Also used by the movement-rule baseline |
| `speed_m_s` | Walking speed. `0.0` while paused or on arrival | Model input. Also used by the movement-rule baseline |
| `heading_rad` | Direction of travel in radians (0 = +x axis, counter-clockwise). While paused it keeps the last walking direction | Model input. Also used by the movement-rule baseline |
| `serving_cell` | Cell ID 0 to 3 serving the user at that second | Model input (current cell). Source of the training label and of handover events |

## Files

| File | Scenario | Trips | Use |
|---|---|---|---|
| `general_train.csv` | general | 300 | Pre-train the GRU |
| `general_val.csv` | general | 50 | Pick the pre-training checkpoint |
| `morning_adapt.csv` | morning rush | 50 | Fine-tune, and train from scratch. Use the first 10, 25 or 50 `trip_id`s |
| `morning_val.csv` | morning rush | 30 | Pick the checkpoint and reservation threshold |
| `morning_test.csv` | morning rush | 100 | Final frozen comparison only |

General trips start anywhere and make 3 legs to random places. Morning trips start at the
hostel and go to a lecture hall about 80% of the time (library or canteen otherwise).
Each file uses its own fixed seed, so no trip appears in two files and reruns give identical data.

## How to read the data

- **Training label.** For row at time `t`, the label is `serving_cell` at `t + 3` in the same trip.
  The last 3 rows of each trip have no label and are dropped.
- **Input window.** The last 5 rows, for example rows at seconds 8 to 12 predict the cell at second 15.
  Only use windows that stay inside one trip.
- **Handover.** Rows where `serving_cell` differs from the previous row of the same trip.
  The label is "no change" most of the time, so evaluate accuracy on windows where the cell changes,
  as the plan says.
- **Quick check.** Plot `x_m` vs `y_m` coloured by `serving_cell` to see the four cell regions
  and the straight paths. Handovers should happen where paths cross the boundaries between them.

## Limitations

- Data is sampled at 1 s, so handover times are rounded to the second. A finer event clock is
  still needed for sub-second preparation-delay evaluation. Calling `make_dataset(..., dt=0.1)`
  with the same seed produces different noisy observations and can change later route choices,
  because sampling consumes random numbers. It cannot supply matching fine-clock events for
  existing one-second CSVs.
- Constant speed, straight segments between via-points and simple Gaussian noise are still easier
  than real movement. Results describe this simulated campus only.

## Person 1 verification

Run the notebook's dataset verification cells after generating the CSVs. The audit checks schema, finite
values, valid cell IDs, consecutive one-second timestamps, complete contiguous trips,
documented trip counts, duplicate trip content across splits, and exact regeneration from
the documented seeds. Trip identity is `(split, trip_id)` because IDs restart in every CSV.
It reports handover counts, training-window counts, changed-target counts, and file hashes.
Serving-cell labels use true positions, so noisy coordinates alone cannot validate those labels.

The existing CSVs support Person 2's training interface. Person 1 still needs the map/sample-route
demo and matching finer-clock handover events for the original preparation-delay plan.
The walking loop also rounds each segment's travel duration to sampling steps, so these
trajectories should be treated as discrete simulation outputs.

## Person 2 setup and training

Use Python 3.11 or newer. On Windows PowerShell, run the environment's Python directly.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Open `train_model.ipynb` in VS Code or Jupyter and select `.venv\Scripts\python.exe` as the
Python kernel. Run All executes dataset generation, verification, training, evaluation,
the held-out demo, and the eight regression checks in order. Each stage has its own cells.
Edit values in the experiment settings cell before training. Keep the notebook working
directory set to this project folder so relative data and checkpoint paths resolve correctly.

The notebook replaces `mobility_simulator.py`, `verify_dataset.py`, `train_model.py`, and
`test_train_model.py`. All definitions are in one notebook, so it can run from a fresh kernel.
The dataset generation cell overwrites the five CSVs using their documented seeds.
The training cells save the checkpoints and results under `artifacts/`.

The model uses five consecutive readings to predict the serving cell three seconds
after the last reading. Features are fixed campus-scaled x/y, scaled speed, sine/cosine of
heading, and a one-hot current cell. Fixed scaling requires no statistics from held-out data.
The model is a single-layer, 32-unit GRU followed by a four-cell classifier, trained with
cross-entropy and Adam. Inference applies softmax to produce four probabilities.

Pre-training uses `general_train`, with the best validation cross-entropy checkpoint selected
using `general_val`. Each fine-tuning experiment independently reloads those pre-trained
weights. Scratch experiments initialize a fresh GRU. Both methods use the same first
10/25/50 adaptation trip IDs, learning rate, maximum adaptation epochs, and early-stopping
rule. `morning_val` selects their checkpoints. All seven frozen checkpoints are then
evaluated on the same `morning_test` windows.

Defaults are 20 pre-training epochs, 40 adaptation epochs, patience 8, batch size 512,
learning rate 0.001, and seed 42. Change these in the experiment settings cell. CPU execution uses
deterministic PyTorch operations and seeded minibatch shuffling. Reproducibility comparisons
should use the same software versions, machine, and thread count. The report stores versions,
configuration, and dataset hashes.

Outputs are saved under `artifacts/`.

- `pretrained.pt`, `finetuned_10.pt`, `scratch_10.pt`, and corresponding 25/50 checkpoints.
- `results.json` with epoch histories, selected epochs, overall and changed-target accuracy,
  and cross-entropy. The current-cell baseline illustrates class imbalance.
- One `*_predictions.csv` per model with probabilities for every eligible held-out window.

Changed-target accuracy uses windows where the cell at `t+3` differs from the current cell.
There are many more unchanged windows, so overall accuracy alone is insufficient.
These are prediction metrics. Reservation thresholds, capacity, and handover delay belong
to the later networking and integration experiments.

### Verified initial run

The default seed-42 run used PyTorch 2.15.0+cpu and NumPy 2.5.5. All 100 held-out
morning trips produced 25,324 windows, including 528 changed-target windows.

| Morning adaptation trips | Scratch changed-target accuracy | Fine-tuned changed-target accuracy |
|---|---|---|
| 10 | 0.0% | 68.2% |
| 25 | 0.0% | 64.2% |
| 50 | 30.1% | 55.9% |

The general pre-trained model reached 76.3% changed-target accuracy. Fine-tuning improved
on scratch training at each budget, while the general checkpoint performed best on this
metric. This single seeded experiment provides no evidence that adaptation improves on
the general checkpoint. Overall accuracy ranged from 97.1% to 98.9%, and always predicting
the current cell achieved 97.9% overall accuracy with 0.0% changed-target accuracy.
Eight regression checks passed, including trip isolation, causal labels, checkpoint reload,
probability output, and exact repetition of a small training run. Every exported model
passed probability-normalization and row-count checks. A full execution of the replacement
notebook on the same machine reproduced all training histories, test metrics, and seven
prediction CSVs exactly. Runs across machines or software versions have not been verified.

### Prediction interface for Persons 3 and 4

The callable interface is available after running the notebook's definition cells.
For integration in a separate program, read the exported `*_predictions.csv` files.

```python
# Run the notebook's GRU definition cells first.
predictor = Predictor("artifacts/finetuned_10.pt")
# readings contains five dictionaries from one trip, in consecutive second order.
probabilities = predictor.predict_proba(readings)
# numpy array of shape (4,). Entry i is P(serving cell at t+3 equals i).
target_cell = int(probabilities.argmax())
confidence = float(probabilities[target_cell])
```

Each dictionary supplies `time_s`, `x_m`, `y_m`, `speed_m_s`, `heading_rad`, and
`serving_cell`. Supply `trip_id` consistently when present. The current serving cell is
available at inference time. Future readings are used only as training/evaluation labels.
The simulator can begin predictions after its fifth reading. The offline prediction CSVs
contain `trip_id,time_s,serving_cell,target_time_s,p_cell_0,p_cell_1,p_cell_2,p_cell_3`.
They cover times `4` through `last_time-3`, when a full input and evaluation target exist.
The callable predictor can also run at the end of a live trip when a future label is unavailable.

### Held-out demonstration

Run the notebook's Held-out trip demo cell after training or loading the saved checkpoints.

This prints every changed-target window in the selected held-out trip, showing the true
future cell, each model's predicted cell, and probability assigned to the true cell before
and after fine-tuning. Edit `trip_id` and set `adaptation_trips` to 10, 25, or 50 in that cell.
Improvements depend on the trip and training run. The demo displays incorrect predictions as well.

The implementation follows the PyTorch [GRU documentation](https://docs.pytorch.org/docs/stable/generated/torch.nn.GRU.html)
and [cross-entropy documentation](https://docs.pytorch.org/docs/stable/generated/torch.nn.CrossEntropyLoss.html).
