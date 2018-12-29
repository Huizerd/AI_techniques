"""
Runs the HMM.
@author: Group 31
"""

from .hmm import *

if __name__ == '__main__':
    training_dir = 'train/'
    test_dir = 'train/'
    possible_moves = ['silent', 'concession', 'unfortunate', 'nice', 'fortunate', 'selfish']
    silent_bound = 0.005

    # Sensor model 1: single agent bids
    sensor_model_simple, state_index, evidence_index = get_sensor_model(training_dir, possible_moves, silent_bound)
    transition_model = get_transition_model(state_index)
    predict(training_dir, sensor_model_simple, transition_model, state_index, evidence_index, silent_bound)

    # Sensor model 2: combined agent bids
    sensor_model, state_index, evidence_index = get_sensor_model(training_dir, possible_moves, silent_bound,
                                                                 combined=True)
    transition_model = get_transition_model(state_index)
    predict(training_dir, sensor_model, transition_model, state_index, evidence_index, silent_bound, combined=True)
