"""Regression tests for local quantum state-vector simulation.

Author: Shuohui Liu
Date: 28 Oct 2026
"""
import unittest
import numpy as np
from local_quantum_simulator import probability_one, sample_probability


def circuit(body, n=2, measured=1):
    return (f'OPENQASM 2.0;\ninclude "qelib1.inc";\nqreg q[{n}];\n'
            f'creg c[1];\n{body}measure q[{measured}] -> c[0];\n')


class LocalSimulatorTests(unittest.TestCase):
    def test_zero_state(self):
        self.assertAlmostEqual(probability_one(circuit('')), 0.0)

    def test_x_on_measured_qubit(self):
        self.assertAlmostEqual(probability_one(circuit('x q[1];\n')), 1.0)

    def test_x_on_other_qubit(self):
        self.assertAlmostEqual(probability_one(circuit('x q[0];\n')), 0.0)

    def test_ry_half_turn(self):
        self.assertAlmostEqual(probability_one(circuit('ry(1.5707963267948966) q[1];\n')), 0.5)

    def test_cx_entanglement(self):
        qasm = circuit('ry(1.5707963267948966) q[0];\ncx q[0],q[1];\n')
        self.assertAlmostEqual(probability_one(qasm), 0.5)

    def test_seeded_sampling(self):
        qasm = circuit('x q[1];\n')
        self.assertEqual(sample_probability(qasm, 20, np.random.default_rng(802)), 1.0)

    def test_unsupported_gate(self):
        with self.assertRaisesRegex(ValueError, 'Unsupported'):
            probability_one(circuit('h q[0];\n'))


if __name__ == '__main__':
    unittest.main()
