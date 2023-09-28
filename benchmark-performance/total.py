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

results['runAlgId'] = results['task'].transform(lambda x: sorter.index(x) if x in sorter else -1)
results['runAlg'] = results.groupby(['systemId', 'startOfRun'])['runAlgId'].transform(lambda x: sorter[x.max()])
#print(results.runAlg.value_counts())
#print(results.accelerationStrategy.value_counts())

# Select only measurements about demosaicing
# results.accelerationStrategy = results.accelerationStrategy.replace('None-OLD', 'Multithreading-OLD') # demosaicing only is implemented with MT in old version
demosaic = results[results.task == 'Total']
#print(demosaic.runAlg.value_counts())
#print(demosaic.accelerationStrategy.value_counts())

# demosaic = demosaic[demosaic.accelerationStrategy != 'GPU (Tile by Tile)'] # no sensible data can be found for this

### ORDER VALUES CONSISTENTLY - see benchmark-accuracy
# sort somewhat intuitively
sorter = [
    # 'NONE',
    # 'NEAREST_NEIGHBOR',
    # 'BICUBIC',
    'BILINEAR_MEAN',
    # 'BILINEAR_MEDIAN',
    # 'PPG',
    # 'MALVAR_HE_CUTLER',
    # 'HAMILTON_ADAMS',
    'RCD',
    # 'DLMMSE_CODE',
    # 'DLMMSE_PAPER',
    # 'DLMMSE_RCD_CODE',
    # 'DLMMSE_RCD_PAPER',
]
sorter.reverse()
# results.task = results.task.astype("category")
# results.task = results.task.cat.set_categories(sorter)

sorter_acc = [
    'Multithreading-OLD',
    'None',
    'Multithreading',
    # 'CPU Tiling',
    # 'CPU Tiling with MT',
    'Thread-distributed CPU Tiling',
    # 'Thread-distributed CPU Tiling with MT',
    'GPU (Tile by Tile)'
]
sorter_acc.reverse()

sorter_acc_no_gpu = [
    'Multithreading-OLD',
    'None',
    'Multithreading',
    'CPU Tiling',
    'CPU Tiling with MT',
    'Thread-distributed CPU Tiling',
    'Thread-distributed CPU Tiling with MT',
]
sorter_acc_no_gpu.reverse()
# results.accelerationStrategy = results.accelerationStrategy.astype("category")
# results.accelerationStrategy = results.accelerationStrategy.cat.set_categories(sorter_acc)

######################
# Algorithm overview #
######################

### this little bit of hackery is necessary to only display tiling with optimal tile size in algorithm overview

# 
demosaic_besttile = demosaic[demosaic.accelerationStrategy.isin(sorter_acc)]
demosaic_besttile = demosaic_besttile[demosaic_besttile.runAlg.isin(sorter)]

min_n_rest = demosaic_besttile.groupby(['runAlg', 'accelerationStrategy']).size().min()
min_n = min_n_rest # min(min_n_tiling, min_n_tiling_mt, min_n_rest, min_n_mt_tiling, min_n_mt_tiling_mt)
# demosaic_besttile = pd.concat([demosaic_besttile, cpu_tiling, cpu_tiling_mt, cpu_mt_tiling, cpu_mt_tiling_mt])


# sort again
demosaic_besttile.runAlg = demosaic_besttile.runAlg.astype("category")
demosaic_besttile.runAlg = demosaic_besttile.runAlg.cat.set_categories(sorter)
demosaic_besttile.accelerationStrategy = demosaic_besttile.accelerationStrategy.astype("category")
demosaic_besttile.accelerationStrategy = demosaic_besttile.accelerationStrategy.cat.set_categories(sorter_acc)

demosaic_avgs_overview = demosaic_besttile.groupby(['runAlg', 'accelerationStrategy']).agg({'taskDuration': ['mean']})

p = demosaic_avgs_overview.unstack().plot.barh()
p.set_title(f'Execution Time Entire Process by Demosaicing Algorithm and Acceleration Strategy (min n/strategy = {min_n})')
reverse_legend_ordering()
plt.show()

################################
# Algorithm overview - speedup #
################################
demosaic_besttile = demosaic_besttile[demosaic_besttile.accelerationStrategy != 'GPU (Operation Wise)']
demosaic_besttile.accelerationStrategy = demosaic_besttile.accelerationStrategy.cat.set_categories(sorter_acc) #_no_gpu)

demosaic_besttile['mean_duration'] = demosaic_besttile.groupby(['runAlg', 'accelerationStrategy'])['taskDuration'].transform(lambda x: x.mean())
# nones = demosaic_besttile[demosaic_besttile.accelerationStrategy == 'None']
# def getNoneDuration(task):
#     return nones[nones.task == task]['mean_duration']

demosaic_besttile['none_duration'] = demosaic_besttile['runAlg'].map(lambda t: (demosaic_besttile[demosaic_besttile.accelerationStrategy == 'None'][demosaic_besttile.runAlg == t]['mean_duration']).iloc[0]).astype('float')
demosaic_besttile['speedup'] = demosaic_besttile['none_duration'] / demosaic_besttile['mean_duration']

demosaic_speedup_overview = demosaic_besttile.groupby(['runAlg', 'accelerationStrategy']).agg({'speedup': ['mean']})

p = demosaic_speedup_overview.unstack().plot.barh()
p.set_title(f'Speedup vs Current Version with no Acceleration (min n/strategy = {min_n})')
reverse_legend_ordering()
plt.show()