# python3 -m pip install pandas matplotlib

import pandas as pd
import matplotlib.pyplot as plt

# helper
def reverse_legend_ordering():
    handles, labels = plt.gca().get_legend_handles_labels()
    handles.reverse()
    labels.reverse()
    plt.legend(handles, labels)

results = pd.read_csv('jeniffer2-logs/timing.csv')
results.accelerationStrategy.fillna(value='None', inplace=True) # String 'None' is parsed as null

results = results[results.accelerationStrategy != 'GPU (Operation Wise)']
results = results[results.accelerationStrategy != 'GPU (Tile by Tile)']

################
# Preprocessor #
################

preprocessor = results[results.task == 'Preprocessing']
# print(preprocessor.description.value_counts())
# exit(0)
min_n = preprocessor.groupby(['accelerationStrategy', 'description']).size().min() # before category, else we get 0 for missing groups

sorter = [
    # '58 tiles of size 1048576',
    '231 tiles of size 262144',
    '924 tiles of size 65536',
    '3693 tiles of size 16384',
    '14770 tiles of size 4096',
    '59079 tiles of size 1024',
]
preprocessor.description = preprocessor.description.astype("category")
preprocessor.description = preprocessor.description.cat.set_categories(sorter)
pre_avgs = preprocessor.groupby(['accelerationStrategy', 'description']).agg({'taskDuration': ['mean']})

p = pre_avgs.unstack().plot.barh()
p.set_title(f'Execution Time Preprocessor (min n/group = {min_n})')
reverse_legend_ordering()
plt.show()

print(pre_avgs)

#################
# Postprocessor #
#################

postprocessor = results[results.task == 'Postprocessing']
min_n = postprocessor.groupby(['accelerationStrategy', 'description']).size().min() # before category, else we get 0 for missing groups
postprocessor.description = postprocessor.description.astype("category")
postprocessor.description = postprocessor.description.cat.set_categories(sorter)

post_avgs = postprocessor.groupby(['accelerationStrategy', 'description']).agg({'taskDuration': ['mean']})

p = pre_avgs.unstack().plot.barh()
p.set_title(f'Execution Time Postprocessor (min n/group = {min_n})')
reverse_legend_ordering()
plt.show()

print(post_avgs)