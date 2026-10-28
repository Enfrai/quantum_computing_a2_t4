# MSE802 Task 4 — Java SDK + Python Notebook

## Setup
Requires Java JDK 17+, Python 3.10+, numpy, scipy, matplotlib, jupyter, nbformat.

```bash
cd java-sdk && bash build.sh && cd ..
python -m pip install -r requirements.txt
jupyter notebook Task4_JavaSDK_Experiment.ipynb
```

Run notebook from the `MSE802_Task4` directory. Set `RUN_QUANTUM = True` in section 4 to attempt live Quokka experiments. Quokka access is not guaranteed; the notebook intentionally defaults to no remote calls. You may override the endpoint using `QUOKKA_URL`.

## Design
- Java JAR: QASM circuit generation, Quokka HTTP transport, classical non-quantum bar/stripe classifier.
- Python: dataset, SciPy optimization, measurement logs, charts, analysis.
- CLI subprocess boundary avoids additional Java/Python bridge dependencies, at the expense of per-call JVM startup overhead.
- Quantum uses the original recursive RY/CNOT circuit and training mean absolute error.
- Classical uses nearest ideal template by Hamming distance. This is a task-level baseline, **not an equivalent replacement for quantum inference**.

## Caveats
- The supplied lab used Quokka URL `http://quokka1.quokkacomputing.com/qsim/qasm`; connectivity and response format may change.
- The 2x2 dataset has only 4 samples and a 75/25 split leaves one test sample.
- Java subprocess overhead and network latency are included in wall-clock time. Compare with care.
- Charts are produced only from real collected measurements. No fabricated quantum output.
- For a rigorous apples-to-apples comparison, use identical test splits, multiple seeds, and a trainable classical baseline.
