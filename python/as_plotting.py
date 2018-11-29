import pandas as pd
import matplotlib.pyplot as plt

FNAME = ['data/own_tournament.csv',
         'data/own_thenegotiator_tournament.csv',
         'data/own_hardheaded_tournament.csv',
         'data/own_gahboninho_tournament.csv']

AGENTS = ['Own',
          'Own_TheNegotiator',
          'Own_HardHeaded',
          'Own_Gahboninho']

# Do all files and corresponding agent variations
for f, a in zip(FNAME, AGENTS):

    # Also look at the other agents
    all_agents = [a, 'TheNegotiator', 'HardHeaded', 'Gahboninho']

    # Import data
    data = pd.read_csv(f, sep=',', header=0, usecols=[12, 13, 14, 15], quotechar='"')

    # Number of occasions where no agreement was reached
    print(data.loc[data['Utility 1'] == 0.0].shape[0])

    for aa in all_agents:

        # Select rows where our agent variation is playing against one of the others
        relevant = data.loc[(data['Agent 1'] == aa) ^ (data['Agent 2'] == aa)]

        # Get utility values of our agent variation
        utils = [row['Utility 1'] if row['Agent 1'] == aa else row['Utility 2'] for _, row in relevant.iterrows()]

        print(f'Average utility of {aa}: {sum(utils) / len(utils)}')
