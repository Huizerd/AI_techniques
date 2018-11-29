import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

FNAME = 'data/domains_tournament_3.csv'

AGENTS = ['Own',
          'TheNegotiator',
          'HardHeaded',
          'Gahboninho']

DOMAINS = ['Party', 'ADG', 'Grocery', 'Laptop']

# Load data
data_raw = pd.read_csv(FNAME, sep=',', header=0, usecols=[2, 4, 9, 10, 12, 13, 14, 15, 16, 17, 20], quotechar='"')

# Clean data
data = data_raw.loc[data_raw['Exception'].isnull()].drop(columns=['Exception'])

# Do all domains
for d in DOMAINS:
    for a in AGENTS:

        # Opponents to count wins
        opponents = [aa for aa in AGENTS if aa != a]

        # Select rows where agent a plays in and where domain is d
        relevant = data.loc[((data['Agent 1'] == a) | (data['Agent 2'] == a)) & (data['Profile 1'].str.extract(r'^([A-Za-z]*)', expand=False).str.lower() == d.lower())]

        # Get wins
        wins = {aa: np.nanmean([1 if (row['Utility 1'] > row['Utility 2'] and row['Agent 1'] == a and row['Agent 2'] == aa) or (row['Utility 2'] > row['Utility 1'] and row['Agent 1'] == aa and row['Agent 2'] == a) else 0 if (row['Utility 1'] < row['Utility 2'] and row['Agent 1'] == a and row['Agent 2'] == aa) or (row['Utility 2'] < row['Utility 1'] and row['Agent 1'] == aa and row['Agent 2'] == a) else np.nan for _, row in relevant.iterrows()]) * 100 for aa in opponents}

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
        print(f'{a} in {d} domain:')
        print(f'Win % {wins}')
        print(f'Average utility: {sum(utils) / len(utils)}')
        print(f'Average discounted utility: {sum(disc_utils) / len(disc_utils)}')
        print(f'Agreement %: {agreed.count("Yes") / len(agreed) * 100}')
        print(f'% on Pareto frontier: {pareto.count(0.0) / len(pareto) * 100}')
        print(f'Average distance to Pareto frontier: {sum(pareto) / len(pareto)}')
        print(f'% in Nash equilibrium: {nash.count(0.0) / len(nash) * 100}')
        print(f'Average distance to Nash equilibrium: {sum(nash) / len(nash)}')
        print()
