import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

FNAME = ['data/own_tournament.csv',
         'data/own_thenegotiator_tournament.csv',
         'data/own_hardheaded_tournament.csv',
         'data/own_gahboninho_tournament.csv']

AGENTS = ['Own',
          'Own_TheNegotiator',
          'Own_HardHeaded',
          'Own_Gahboninho']

OPPONENTS = ['TheNegotiator',
             'HardHeaded',
             'Gahboninho']

# Do all files and corresponding agent variations
for f, a in zip(FNAME, AGENTS):

    # Load data
    data_raw = pd.read_csv(f, sep=',', header=0, usecols=[2, 4, 9, 10, 12, 13, 14, 15, 16, 17], quotechar='"')

    # Clean data
    data = data_raw.loc[data_raw['Exception'].isnull()].drop(columns=['Exception'])

    # Select rows where our agent variation is playing against one of the others
    relevant = data.loc[(data['Agent 1'] == a) ^ (data['Agent 2'] == a)]

    # Get wins
    wins = {aa: np.nanmean([1 if (row['Utility 1'] > row['Utility 2'] and row['Agent 1'] == a and row['Agent 2'] == aa) or (row['Utility 2'] > row['Utility 1'] and row['Agent 1'] == aa and row['Agent 2'] == a) else 0 if (row['Utility 1'] < row['Utility 2'] and row['Agent 1'] == a and row['Agent 2'] == aa) or (row['Utility 2'] < row['Utility 1'] and row['Agent 1'] == aa and row['Agent 2'] == a) else np.nan for _, row in relevant.iterrows()]) * 100 for aa in OPPONENTS}

    # Get (discounted) utility values of our agent variation
    utils = [(row['Utility 1'] + row['Utility 2']) / 2 if row['Agent 1'] == a and row['Agent 2'] == a else row['Utility 1'] if row['Agent 1'] == a else row['Utility 2'] for _, row in relevant.iterrows()]
    disc_utils = [(row['Disc. Util. 1'] + row['Disc. Util. 2']) / 2 if row['Agent 1'] == a and row['Agent 2'] == a else row['Disc. Util. 1'] if row['Agent 1'] == a else row['Disc. Util. 2'] for _, row in relevant.iterrows()]

    # Agreements
    agreed = relevant['Agreement'].tolist()

    # Get Pareto proximity
    pareto = relevant['Dist. To Pareto'].tolist()

    # Get Nash proximity
    nash = relevant['Dist. To Nash'].tolist()

    # Print results
    print(f'{a}:')
    print(f'Win % {wins}')
    print(f'Average utility: {sum(utils) / len(utils)}')
    print(f'Agreement %: {agreed.count("Yes") / len(agreed) * 100}')
    print(f'% on Pareto frontier: {pareto.count(0.0) / len(pareto) * 100}')
    print(f'Average distance to Pareto frontier: {sum(pareto) / len(pareto)}')
    print(f'% in Nash equilibrium: {nash.count(0.0) / len(nash) * 100}')
    print(f'Average distance to Nash equilibrium: {sum(nash) / len(nash)}')
    print()
    