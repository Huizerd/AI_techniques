"""
Contains the HMM functions.
@author: Group 31
"""

import json
import os
import re
from collections import Counter, OrderedDict

import numpy as np


# Go from bid -> utility
def get_utility(bid, pref):
    util = 0
    issuelist = list(pref.keys())
    for issueindex, issue in enumerate(issuelist):
        # values of issue in given bid
        value = pref[issue][bid[issueindex]]
        # weight of issue in given bid
        weight = pref[issue]['weight']
        util += weight * value

    return util


# Utility change to discrete move type
def delta_mapping(delta_util, silent_bound=0.01):
    if (abs(delta_util[0]) <= silent_bound and abs(delta_util[1]) <= silent_bound):
        return 'silent'
    if (abs(delta_util[0]) <= silent_bound and delta_util[1]) > silent_bound:
        return 'nice'
    if (delta_util[0] <= 0 and delta_util[1] <= 0):
        return 'unfortunate'
    if (delta_util[0] > 0 and delta_util[1] <= 0):
        return 'selfish'
    if (delta_util[0] > 0 and delta_util[1] > 0):
        return 'fortunate'
    if (delta_util[0] <= 0 and delta_util[1] > 0):
        return 'concession'


# List of bids to list of discrete moves
def discritized_mapping(agent_bids, silent_bound=0.01):
    mapped_utils_discrete = []
    prev_utils = agent_bids[0]

    for new_utils in agent_bids[1:]:
        delta_util1 = new_utils[0] - prev_utils[0]
        delta_util2 = new_utils[1] - prev_utils[1]
        delta_util = (delta_util1, delta_util2)
        mapped_utils_discrete.append(delta_mapping(delta_util, silent_bound))
        prev_utils = new_utils

    return mapped_utils_discrete


def retrieve_all_agents_bids(train, silent_bound=0.01):
    # Useful structures
    all_issues = train['issues']
    pref1 = train['Utility1']
    pref2 = train['Utility2']
    all_bids = train['bids']

    mapped_utils_a1 = []
    mapped_utils_a2 = []

    # Parse utility values of bids
    for bid in all_bids:
        r = bid['round']
        # stop if the negotiation session has ended
        if 'agent1' in bid:
            bid_agent1 = bid['agent1'].split(',')
            u1_b1 = get_utility(bid_agent1, pref1)
            u2_b1 = get_utility(bid_agent1, pref2)
            # Save the bid -> utility mapping
            mapped_utils_a1.append([u1_b1, u2_b1, int(r)])
        if 'agent2' in bid:
            bid_agent2 = bid['agent2'].split(',')
            u1_b2 = get_utility(bid_agent2, pref1)
            u2_b2 = get_utility(bid_agent2, pref2)
            mapped_utils_a2.append([u2_b2, u1_b2, int(r)])

    agent1_bids = [mapped_utils_a1[i][0:2] for i in range(len(mapped_utils_a1))]
    agent2_bids = [mapped_utils_a2[i][0:2] for i in range(len(mapped_utils_a2))]

    # Discretize bidspace
    agent1_bids_discrete = discritized_mapping(agent1_bids, silent_bound)
    agent2_bids_discrete = discritized_mapping(agent2_bids, silent_bound)

    return (agent1_bids_discrete, agent2_bids_discrete)


def get_bids(training_dir, silent_bound=0.01, combined=False):
    train_files = os.listdir(training_dir)
    agent_count_mapping = {}
    for t in train_files:
        train = json.load(open(os.path.join(training_dir, t)))
        a1_name, a2_name = re.split(r'[^A-Za-z]+', t.strip('.json'))[0:2]
        a1_bids, a2_bids = retrieve_all_agents_bids(train, silent_bound)

        if a1_name not in agent_count_mapping:
            agent_count_mapping[a1_name] = []
        if a2_name not in agent_count_mapping:
            agent_count_mapping[a2_name] = []

        if combined:
            a1_com = [a1_bids[i] + a2_bids[i] for i in range(min(len(a1_bids), len(a2_bids)))]
            a2_com = [a2_bids[i] + a1_bids[i] for i in range(min(len(a1_bids), len(a2_bids)))]

            agent_count_mapping[a1_name] = agent_count_mapping[a1_name] + a1_com
            agent_count_mapping[a2_name] = agent_count_mapping[a2_name] + a2_com
        else:
            agent_count_mapping[a1_name] = agent_count_mapping[a1_name] + a1_bids
            agent_count_mapping[a2_name] = agent_count_mapping[a2_name] + a2_bids

    return agent_count_mapping


def get_sensor_model(training_dir, possible_moves, silent_bound=0.01, combined=False):
    agent_count_mapping = get_bids(training_dir, silent_bound, combined)

    if combined:
        dummy = possible_moves[:]
        possible_moves = []

        for m1 in dummy:
            for m2 in dummy:
                possible_moves.append(m1 + m2)

    # Start constructing the model.
    sensor_model = {}
    for k, v in agent_count_mapping.items():
        cnt_bids = Counter(v)
        total = len(v)
        for key in cnt_bids:
            cnt_bids[key] /= total
        sensor_model[k] = dict(cnt_bids)

    for k, moves in sensor_model.items():
        for pm in possible_moves:
            if pm not in moves:
                moves[pm] = 0.0
        sensor_model[k] = np.array(list(dict(OrderedDict(sorted(moves.items()))).values()))

    evidence_index = {k: v for v, k in enumerate(sorted(possible_moves))}
    state_index = {k: v for v, k in enumerate(list(sensor_model.keys()))}
    sensor_model = np.array(list(sensor_model.values()))

    return sensor_model, state_index, evidence_index


def get_transition_model(states):
    return np.identity(len(states))


def predict(training_dir, sensor_model, transition_model, state_index, evidence_index, combined=False):
    train_files = os.listdir(training_dir)
    labels_keys = list(state_index.keys())

    labels = []
    preds = []

    for t in train_files:
        train = json.load(open(os.path.join(training_dir, t)))
        a1_name, a2_name = re.split(r'[^A-Za-z]+', t.strip('.json'))[0:2]
        a1_bids, a2_bids = retrieve_all_agents_bids(train)
        labels.append(a1_name)
        labels.append(a2_name)

        if combined:
            a1_com = [a1_bids[i] + a2_bids[i] for i in range(min(len(a1_bids), len(a2_bids)))]
            a2_com = [a2_bids[i] + a1_bids[i] for i in range(min(len(a1_bids), len(a2_bids)))]
        else:
            a1_com = a1_bids
            a2_com = a2_bids

        p1 = filter(a1_com, sensor_model, transition_model, evidence_index)
        p2 = filter(a2_com, sensor_model, transition_model, evidence_index)

        preds.append(labels_keys[np.argmax(p1)])
        preds.append(labels_keys[np.argmax(p2)])

        print(f'P1 (predicted, true): ({labels_keys[np.argmax(p1)]}, {a1_name}) --- '
              f'P2 (predicted, true): ({labels_keys[np.argmax(p2)]}, {a2_name})')

    c = [labels[x] == preds[x] for x in range(len(labels))]
    print(f'\nOverall accuracy: {sum(c) / float(len(labels))}\n\n')


def filter(bids, sensor_model, transition_model, evidence_index):
    Bt = np.ones(4) * 0.25

    for t, bt in enumerate(bids):
        sm1 = sensor_model[:, evidence_index[bt]]
        stm1 = np.dot(transition_model, Bt)
        Bt = np.multiply(sm1, stm1)
        Bt = Bt / sum(Bt)

    return Bt
