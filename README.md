# Campus Mobility

A notebook that simulates campus trips, trains a GRU, and compares fine-tuning with scratch training using 10, 25, and 50 morning trips.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Open `train_model.ipynb`, select the `.venv` Python kernel, and Run All from this folder. Edit settings in the first code cell.

The model uses five readings to predict the serving cell three seconds ahead. Evaluation uses separate test trips.

Generated trips go to `data/`. Checkpoints, prediction CSVs, and `results.json` go to `artifacts/`. These folders are ignored by Git.

Handovers have one-second resolution. Sub-second events and the campus-map demo are pending.
