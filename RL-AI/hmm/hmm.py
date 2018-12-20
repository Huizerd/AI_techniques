"""
Contains the HMM class.
@author: Group 31
"""

import os
import re
import json

import numpy as np


class HMM:
    """
    Contains the HMM structure and functions.
    """

    def __init__(self, states, observations):
        """
        HMM initialization.
        :param state: List of possible states.
        :param observations: List of possible observations.
        """

        # Create a mapping from states to indices
        self.states = {state: i for i, state in enumerate(states)}

        # Create a mapping from observations to indices
        self.observations = {obs: i for i, obs in enumerate(observations)}

        # Initialize state probabilities
        self.init_states = [1 / len(states) for _ in states]

        # Initialize transition matrix
        self.transition_matrix = np.identity(len(states))

        # Initialize observation matrix
        self.observation_matrix = 1 / len(observations) * np.ones((len(states), len(observations)))

    def fit(self, training_dir, parties, silent_thres=0.005):
        """
        Fit the HMM.
        :param training_dir: Location of the negotiation session training logs (JSON files).
        :param parties: List of negotiation parties taking part.
        :param silent_thres: Threshold for classifying silent moves.
        :return:
        """

        # Retrieve all negotiation sessions
        sessions = self.get_sessions(training_dir)

        # Classify party bids for all sessions
        party_counter = self.classify_bids(sessions, parties)

    def get_sessions(self, logs_dir):
        """
        Retrieves negotiation sessions from a certain directory.
        :param training_dir: Location of the negotiation session logs (JSON files).
        :return: List negotiation session dictionaries.
        """

        # Initialize sessions
        sessions = []

        # List all logs
        logs = os.listdir(logs_dir)

        # Go over all
        for l in logs:
            with open(os.path.join(logs_dir, l)) as f:
                sessions.append({'parties': re.split(r'[^A-Za-z]+', l)[0:2], 'data': json.load(f)})

        return sessions

    def classify_bids(self, sessions, parties):
        """
        Classifies bids of parties into categories over multiple negotiation sessions.
        :param sessions: List of negotiation session dictionaries.
        :param parties: List of negotiation parties taking part.
        :return: Counter of bid categories for each party.
        """

        # Counter of bid types for each party
        party_counter = {party: [] for party in parties}

        # Loop over all sessions
        for sess in sessions:
            # Extract names and bid lists (which also classifies bids)
            party1_name, party2_name = sess['parties'][0:2]
            party1_bids, party2_bids = get_bids(sess)

            # Update counter
            party_counter[party1_name] += party1_bids
            party_counter[party2_name] += party2_bids

        return party_counter

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
    def delta_mapping(delta_util):
        if (abs(delta_util[0]) <= bound and abs(delta_util[1]) <= bound):
            return 'silent'
        if (abs(delta_util[0]) <= bound and delta_util[1]) > bound:
            return 'nice'
        if (delta_util[0] <= 0 and delta_util[1] <= 0):
            return 'unfortunate'
        if (delta_util[0] > 0 and delta_util[1] <= 0):
            return 'selfish'
        if (delta_util[0] > 0 and delta_util[1] > 0):
            return 'fortunate'
        if (delta_util[0] <= 0 and delta_util[1] > 0):
            return 'concession'
        print(delta_util)

    # List of bids to list of discrete moves
    def discritized_mapping(agent_bids):
        mapped_utils_discrete = []
        prev_utils = agent_bids[0]
        for new_utils in agent_bids[1:]:
            delta_util1 = new_utils[0] - prev_utils[0]
            delta_util2 = new_utils[1] - prev_utils[1]
            delta_util = (delta_util1, delta_util2)
            mapped_utils_discrete.append(delta_mapping(delta_util))
            prev_utils = new_utils
        return mapped_utils_discrete

    def retrieve_all_agents_bids(train):
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
            #         print(bid)
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

        # Discritize bidspace
        agent1_bids_discrete = discritized_mapping(agent1_bids)
        agent2_bids_discrete = discritized_mapping(agent2_bids)

        return (agent1_bids_discrete, agent2_bids_discrete)
