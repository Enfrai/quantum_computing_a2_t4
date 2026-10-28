"""Local OpenQASM 2 state-vector simulator for the Task 4 Java-generated circuits.

Author: Shuohui Liu
Date: 28 Oct 2026

Supports only X, RY, CX, and a final single-qubit measurement. Qubit 0
is the least significant state-vector bit, matching OpenQASM indexing.
This is classical simulation of a quantum circuit, not quantum hardware.
"""
import re
import numpy as np

_X = re.compile(r"^x q\[(\d+)\];$")
_RY = re.compile(r"^ry\(([^)]+)\) q\[(\d+)\];$")
_CX = re.compile(r"^cx q\[(\d+)\],q\[(\d+)\];$")
_MEASURE = re.compile(r"^measure q\[(\d+)\] -> c\[0\];$")


def probability_one(qasm: str) -> float:
    """Return the exact probability of measuring 1 on the circuit's measured qubit."""
    match = re.search(r"^qreg q\[(\d+)\];$", qasm, re.MULTILINE)
    if not match:
        raise ValueError("Missing qreg declaration")
    n = int(match.group(1))
    if not 1 <= n <= 16:
        raise ValueError("Local simulator supports 1 to 16 qubits")
    state = np.zeros(1 << n, dtype=np.complex128)
    state[0] = 1
    measured = None
    for raw in qasm.splitlines():
        line = raw.strip()
        if not line or line.startswith(("OPENQASM", "include", "qreg", "creg", "//")):
            continue
        if m := _X.fullmatch(line):
            q = int(m.group(1))
            _check(q, n)
            for i in range(len(state)):
                if not (i & (1 << q)):
                    j = i | (1 << q)
                    state[i], state[j] = state[j], state[i]
        elif m := _RY.fullmatch(line):
            theta, q = float(m.group(1)), int(m.group(2))
            _check(q, n)
            c, s = np.cos(theta / 2), np.sin(theta / 2)
            for i in range(len(state)):
                if not (i & (1 << q)):
                    j = i | (1 << q)
                    a, b = state[i], state[j]
                    state[i], state[j] = c * a - s * b, s * a + c * b
        elif m := _CX.fullmatch(line):
            control, target = int(m.group(1)), int(m.group(2))
            _check(control, n)
            _check(target, n)
            if control == target:
                raise ValueError("CX control and target must differ")
            for i in range(len(state)):
                if (i & (1 << control)) and not (i & (1 << target)):
                    j = i | (1 << target)
                    state[i], state[j] = state[j], state[i]
        elif m := _MEASURE.fullmatch(line):
            if measured is not None:
                raise ValueError("Only one measurement supported")
            measured = int(m.group(1))
            _check(measured, n)
        else:
            raise ValueError(f"Unsupported QASM instruction: {line}")
    if measured is None:
        raise ValueError("Missing measurement")
    return float(np.clip(sum(abs(a) ** 2 for i, a in enumerate(state)
                             if i & (1 << measured)), 0, 1))


def _check(q: int, n: int) -> None:
    """Validate a qubit index."""
    if not 0 <= q < n:
        raise ValueError(f"Invalid qubit index: {q}")


def sample_probability(qasm: str, shots: int, rng: np.random.Generator) -> float:
    """Sample measurement outcomes and return their empirical mean."""
    if shots <= 0:
        raise ValueError("shots must be positive")
    return float(rng.binomial(shots, probability_one(qasm)) / shots)
