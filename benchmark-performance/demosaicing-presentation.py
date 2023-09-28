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

# Translate different algorithm names (from old and GPU)
results.task = results.task.replace({'DLMMSE_RCD': 'DLMMSE_RCD_CODE', 'GPURCD': 'RCD', 'GPUBilinearMean': 'BILINEAR_MEAN'})

# Select only measurements about demosaicing
results.accelerationStrategy = results.accelerationStrategy.replace('None-OLD', 'Multithreading-OLD') # demosaicing only is implemented with MT in old version
demosaic = results[results.task.isin('NONE NEAREST_NEIGHBOR BILINEAR_MEAN BILINEAR_MEDIAN BICUBIC MALVAR_HE_CUTLER HAMILTON_ADAMS PPG RCD DLMMSE_CODE DLMMSE_PAPER DLMMSE_RCD_CODE DLMMSE_RCD_PAPER GPUBilinearMean GPURCD'.split(' '))]
demosaic = demosaic[demosaic.accelerationStrategy != 'GPU (Tile by Tile)'] # no sensible data can be found for this

# not needed for concise presentation
demosaic = demosaic[demosaic.accelerationStrategy != 'CPU Tiling']
demosaic = demosaic[demosaic.accelerationStrategy != 'Thread-distributed CPU Tiling with MT']
demosaic = demosaic[demosaic.accelerationStrategy != 'CPU Tiling with MT']

### ORDER VALUES CONSISTENTLY - see benchmark-accuracy
# sort somewhat intuitively
sorter = [
    'NONE',
    'NEAREST_NEIGHBOR',
    'BICUBIC',
    'BILINEAR_MEAN',
    'BILINEAR_MEDIAN',
    'PPG',
    'MALVAR_HE_CUTLER',
    'HAMILTON_ADAMS',
    'RCD',
    'DLMMSE_CODE',
    'DLMMSE_PAPER',
    'DLMMSE_RCD_CODE',
    'DLMMSE_RCD_PAPER',
]
sorter.reverse()
# results.task = results.task.astype("category")
# results.task = results.task.cat.set_categories(sorter)

sorter_acc = [
    'Multithreading-OLD',
    'None',
    'Multithreading',
    'Thread-distributed CPU Tiling',
    'GPU (Operation Wise)'
]
sorter_acc.reverse()

sorter_acc_no_gpu = [
    'Multithreading-OLD',
    'None',
    'Multithreading',
    'Thread-distributed CPU Tiling'
]
sorter_acc_no_gpu.reverse()
# results.accelerationStrategy = results.accelerationStrategy.astype("category")
# results.accelerationStrategy = results.accelerationStrategy.cat.set_categories(sorter_acc)

######################
# Algorithm overview #
######################

### this little bit of hackery is necessary to only display tiling with optimal tile size in algorithm overview

## Thread-distributed CPU Tiling ##
cpu_tiling_mt = demosaic[demosaic.accelerationStrategy == 'Thread-distributed CPU Tiling']
demosaic_besttile = demosaic[demosaic.accelerationStrategy != 'Thread-distributed CPU Tiling']

min_n_tiling_mt = cpu_tiling_mt.groupby(['task', 'description']).size().min()

cpu_tiling_mt['taskDuration'] = cpu_tiling_mt.groupby(['task', 'description'])['taskDuration'].transform(lambda x: x.mean())
# print(cpu_tiling)
cpu_tiling_mt['taskDuration'] = cpu_tiling_mt.groupby(['task'])['taskDuration'].transform(lambda x: x.min())


min_n_rest = demosaic_besttile.groupby(['task', 'accelerationStrategy']).size().min()
min_n = min(min_n_tiling_mt, min_n_rest)
demosaic_besttile = pd.concat([demosaic_besttile, cpu_tiling_mt])


# sort again
demosaic_besttile.task = demosaic_besttile.task.astype("category")
demosaic_besttile.task = demosaic_besttile.task.cat.set_categories(sorter)
demosaic_besttile.accelerationStrategy = demosaic_besttile.accelerationStrategy.astype("category")
demosaic_besttile.accelerationStrategy = demosaic_besttile.accelerationStrategy.cat.set_categories(sorter_acc)

demosaic_avgs_overview = demosaic_besttile.groupby(['task', 'accelerationStrategy']).agg({'taskDuration': ['mean']})

p = demosaic_avgs_overview.unstack().plot.barh()
p.set_title(f'Execution Time Demosaicing Algorithms (min n/strategy = {min_n})')

# reverse weird default order
reverse_legend_ordering()

plt.show()

################################
# Algorithm overview - speedup #
################################
demosaic_besttile = demosaic_besttile[demosaic_besttile.accelerationStrategy != 'GPU (Operation Wise)']
demosaic_besttile.accelerationStrategy = demosaic_besttile.accelerationStrategy.cat.set_categories(sorter_acc_no_gpu)

demosaic_besttile['mean_duration'] = demosaic_besttile.groupby(['task', 'accelerationStrategy'])['taskDuration'].transform(lambda x: x.mean())
# nones = demosaic_besttile[demosaic_besttile.accelerationStrategy == 'None']
# def getNoneDuration(task):
#     return nones[nones.task == task]['mean_duration']

demosaic_besttile['none_duration'] = demosaic_besttile['task'].map(lambda t: (demosaic_besttile[demosaic_besttile.accelerationStrategy == 'None'][demosaic_besttile.task == t]['mean_duration']).iloc[0]).astype('float')
demosaic_besttile['speedup'] = demosaic_besttile['none_duration'] / demosaic_besttile['mean_duration']

demosaic_speedup_overview = demosaic_besttile.groupby(['task', 'accelerationStrategy']).agg({'speedup': ['mean']})

p = demosaic_speedup_overview.unstack().plot.barh()
p.set_title(f'Speedup vs Current Version with no Acceleration (min n/strategy = {min_n})')

# reverse weird default order
reverse_legend_ordering()

plt.show()