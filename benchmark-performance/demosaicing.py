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
    'CPU Tiling',
    'CPU Tiling with MT',
    'Thread-distributed CPU Tiling',
    'Thread-distributed CPU Tiling with MT',
    'GPU (Operation Wise)'
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

## CPU Tiling ##
cpu_tiling = demosaic[demosaic.accelerationStrategy == 'CPU Tiling']
demosaic_besttile = demosaic[demosaic.accelerationStrategy != 'CPU Tiling']

min_n_tiling = cpu_tiling.groupby(['task', 'description']).size().min()

cpu_tiling['taskDuration'] = cpu_tiling.groupby(['task', 'description'])['taskDuration'].transform(lambda x: x.mean())
# print(cpu_tiling)
cpu_tiling['taskDuration'] = cpu_tiling.groupby(['task'])['taskDuration'].transform(lambda x: x.min())
# print(cpu_tiling)

## Thread-distributed CPU Tiling with MT ##
cpu_mt_tiling_mt = demosaic[demosaic.accelerationStrategy == 'Thread-distributed CPU Tiling with MT']
demosaic_besttile = demosaic_besttile[demosaic_besttile.accelerationStrategy != 'Thread-distributed CPU Tiling with MT']

min_n_mt_tiling_mt = cpu_mt_tiling_mt.groupby(['task', 'description']).size().min()

cpu_mt_tiling_mt['taskDuration'] = cpu_mt_tiling_mt.groupby(['task', 'description'])['taskDuration'].transform(lambda x: x.mean())
# print(cpu_tiling)
cpu_mt_tiling_mt['taskDuration'] = cpu_mt_tiling_mt.groupby(['task'])['taskDuration'].transform(lambda x: x.min())

## CPU Tiling with MT ##
cpu_mt_tiling = demosaic[demosaic.accelerationStrategy == 'CPU Tiling with MT']
demosaic_besttile = demosaic_besttile[demosaic_besttile.accelerationStrategy != 'CPU Tiling with MT']

min_n_mt_tiling = cpu_mt_tiling.groupby(['task', 'description']).size().min()

cpu_mt_tiling['taskDuration'] = cpu_mt_tiling.groupby(['task', 'description'])['taskDuration'].transform(lambda x: x.mean())
# print(cpu_tiling)
cpu_mt_tiling['taskDuration'] = cpu_mt_tiling.groupby(['task'])['taskDuration'].transform(lambda x: x.min())

## Thread-distributed CPU Tiling ##
cpu_tiling_mt = demosaic[demosaic.accelerationStrategy == 'Thread-distributed CPU Tiling']
demosaic_besttile = demosaic_besttile[demosaic_besttile.accelerationStrategy != 'Thread-distributed CPU Tiling']

min_n_tiling_mt = cpu_tiling_mt.groupby(['task', 'description']).size().min()

cpu_tiling_mt['taskDuration'] = cpu_tiling_mt.groupby(['task', 'description'])['taskDuration'].transform(lambda x: x.mean())
# print(cpu_tiling)
cpu_tiling_mt['taskDuration'] = cpu_tiling_mt.groupby(['task'])['taskDuration'].transform(lambda x: x.min())


min_n_rest = demosaic_besttile.groupby(['task', 'accelerationStrategy']).size().min()
min_n = min(min_n_tiling, min_n_tiling_mt, min_n_rest, min_n_mt_tiling, min_n_mt_tiling_mt)
demosaic_besttile = pd.concat([demosaic_besttile, cpu_tiling, cpu_tiling_mt, cpu_mt_tiling, cpu_mt_tiling_mt])


# sort again
demosaic_besttile.task = demosaic_besttile.task.astype("category")
demosaic_besttile.task = demosaic_besttile.task.cat.set_categories(sorter)
demosaic_besttile.accelerationStrategy = demosaic_besttile.accelerationStrategy.astype("category")
demosaic_besttile.accelerationStrategy = demosaic_besttile.accelerationStrategy.cat.set_categories(sorter_acc)

demosaic_avgs_overview = demosaic_besttile.groupby(['task', 'accelerationStrategy']).agg({'taskDuration': ['mean']})

p = demosaic_avgs_overview.unstack().plot.barh()
p.set_title(f'Execution Time Demosaicing Algorithms (min n/strategy = {min_n})')
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
reverse_legend_ordering()
plt.show()


##########################
# Tiling Tuning Sideways #
##########################

fig, ((p1, p2, p3, p4)) = plt.subplots(4, 1)

# changes for both
demosaic = demosaic[demosaic.description != '100000']

## CPU Tiling

demosaic_cpu_tiling = demosaic[demosaic.accelerationStrategy == 'CPU Tiling']

demosaic_cpu_tiling.description = demosaic_cpu_tiling.description.astype("int64")

demosaic_cpu_tiling.task = demosaic_cpu_tiling.task.astype("category")
demosaic_cpu_tiling.task = demosaic_cpu_tiling.task.cat.set_categories(sorter)

cpu_tiling_avgs = demosaic_cpu_tiling.groupby(['task', 'description']).agg({'taskDuration': ['mean']})

cpu_tiling_avgs = cpu_tiling_avgs.groupby('task').transform(lambda x: x.max() / x)

# p = cpu_tiling_avgs.unstack().plot.bar()
p = cpu_tiling_avgs.unstack().plot(kind='bar', colormap='turbo', ax=p1)
p.set_title(f'CPU Tiling (min n = {min_n_tiling})', loc='left')
p.set(xticklabels=[])
reverse_legend_ordering()
p.legend(
    bbox_to_anchor=(0.85, 1.0),
    fontsize='small',
)

## CPU Tiling with MT

demosaic_cpu_tiling_mt = demosaic[demosaic.accelerationStrategy == 'CPU Tiling with MT']

demosaic_cpu_tiling_mt.description = demosaic_cpu_tiling_mt.description.astype("int64")

demosaic_cpu_tiling_mt.task = demosaic_cpu_tiling_mt.task.astype("category")
demosaic_cpu_tiling_mt.task = demosaic_cpu_tiling_mt.task.cat.set_categories(sorter)

cpu_tiling_avgs_mt = demosaic_cpu_tiling_mt.groupby(['task', 'description']).agg({'taskDuration': ['mean']})

cpu_tiling_avgs_mt = cpu_tiling_avgs_mt.groupby('task').transform(lambda x: x.max() / x)

p = cpu_tiling_avgs_mt.unstack().plot(kind='bar', colormap='turbo', ax=p2, legend=False)
p.set_title(f'CPU Tiling with MT (min n = {min_n_mt_tiling})', loc='left')
p.set(xticklabels=[])
## Thread-distributed CPU Tiling

demosaic_cpu_tiling_mt = demosaic[demosaic.accelerationStrategy == 'Thread-distributed CPU Tiling']

demosaic_cpu_tiling_mt.description = demosaic_cpu_tiling_mt.description.astype("int64")

demosaic_cpu_tiling_mt.task = demosaic_cpu_tiling_mt.task.astype("category")
demosaic_cpu_tiling_mt.task = demosaic_cpu_tiling_mt.task.cat.set_categories(sorter)

cpu_tiling_avgs_mt = demosaic_cpu_tiling_mt.groupby(['task', 'description']).agg({'taskDuration': ['mean']})

cpu_tiling_avgs_mt = cpu_tiling_avgs_mt.groupby('task').transform(lambda x: x.max() / x)

p = cpu_tiling_avgs_mt.unstack().plot(kind='bar', colormap='turbo', ax=p3, legend=False)
p.set_title(f'Thread-distributed CPU Tiling (min n = {min_n_tiling_mt})', loc='left')
p.set(xticklabels=[])

## Thread-distributed CPU Tiling with MT

demosaic_cpu_tiling_mt = demosaic[demosaic.accelerationStrategy == 'Thread-distributed CPU Tiling with MT']

demosaic_cpu_tiling_mt.description = demosaic_cpu_tiling_mt.description.astype("int64")

demosaic_cpu_tiling_mt.task = demosaic_cpu_tiling_mt.task.astype("category")
demosaic_cpu_tiling_mt.task = demosaic_cpu_tiling_mt.task.cat.set_categories(sorter)

cpu_tiling_avgs_mt = demosaic_cpu_tiling_mt.groupby(['task', 'description']).agg({'taskDuration': ['mean']})

cpu_tiling_avgs_mt = cpu_tiling_avgs_mt.groupby('task').transform(lambda x: x.max() / x)

p = cpu_tiling_avgs_mt.unstack().plot(kind='bar', colormap='turbo', ax=p4, rot=45, legend=False)
p.set_title(f'Thread-distributed CPU Tiling with MT (min n = {min_n_mt_tiling_mt})', loc='left')

fig.suptitle('Speedup abh. von Tile Size, relativ zu längster Zeit', fontsize=16)
plt.show()