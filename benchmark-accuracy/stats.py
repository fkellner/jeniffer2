# python3 -m pip install pandas matplotlib

import pandas as pd
import matplotlib.pyplot as plt

results = pd.read_csv('benchmark.csv')

averages = results.groupby(['alg', 'dataset']).agg({'mse':['mean'], 'psnr':['mean'], 'mssim':['mean']})

print(averages)

#############################
# Compare Reimplementations #
#############################

# group implementations of same algorithm together
sorter = [
    'PPG-OLD',
    'PPG',

    'RCD',
    'RCD_REWRITE',
    'RCD-GPU',

    'DLMMSE_CODE',
    'DLMMSE_REWRITE_CODE',

    'DLMMSE_PAPER',
    'DLMMSE_REWRITE_PAPER',

    'DLMMSE_RCD',
    'DLMMSE_RCD_REWRITE',

    'DLMMSE_RCD_PAPER',
    'DLMMSE_RCD_REWRITE_PAPER'
]
sorter.reverse()
reimplementations = results[results.alg.isin(sorter)]
reimplementations.alg = reimplementations.alg.astype("category")
reimplementations.alg = reimplementations.alg.cat.set_categories(sorter)

fig, ((p1) , (p2), (p3)) = plt.subplots(3, 1)
p = reimplementations.boxplot(column=['mse'], by=['alg'], vert=False, ax=p1)
p.set_title('Mean Square Error')
p.set_xlabel('')
p.set_ylabel('')

p = reimplementations.boxplot(column=['psnr'], by=['alg'], vert=False, ax=p2)
p.set_title('Peak Signal to Noise Ratio')
p.set_xlabel('')
p.set_ylabel('')
# p.set(yticklabels=[])

p = reimplementations.boxplot(column=['mssim'], by=['alg'], vert=False, ax=p3)
p.set_title('Mean Structural Similarity Index Measure')
p.set_xlabel('')
p.set_ylabel('')

fig.suptitle('Überblick Akkuratheit reimplementierter Algorithmen (n = 57)', fontsize=16)
fig.set_size_inches(8.27,11.69)
plt.subplots_adjust(left=0.3, right=0.95, bottom=0.05)
fig.savefig('accuracy-reimplementations.png', format='png')
# plt.show()


######################
# Algorithm overview #
######################

# sort somewhat intuitively
sorter = [
    'NEAREST_NEIGHBOR',
    'BICUBIC',
    'BILINEAR_MEAN',
    'BILINEAR_MEDIAN',
    'PPG',
    'MalvarHeCutler',
    'HAMILTON_ADAMS',
    'RCD',
    'DLMMSE_CODE',
    'DLMMSE_PAPER',
    'DLMMSE_RCD',
    'DLMMSE_RCD_PAPER',
]
sorter.reverse()
overview = results[results.alg.isin(sorter)]
overview.alg = overview.alg.astype("category")
overview.alg = overview.alg.cat.set_categories(sorter)

fig, ((p1), (p2), (p3)) = plt.subplots(3, 1)
p = overview.boxplot(column=['mse'], by=['alg'], vert=False, ax=p1)
p.set_title('Mean Square Error')
p.set_xlabel('')
p.set_ylabel('')

p = overview.boxplot(column=['psnr'], by=['alg'], vert=False, ax=p2)
p.set_title('Peak Signal to Noise Ratio')
p.set_xlabel('')
p.set_ylabel('')
# p.set(yticklabels=[])

p = overview.boxplot(column=['mssim'], by=['alg'], vert=False, ax=p3)
p.set_title('Mean Structural Similarity Index Measure')
p.set_xlabel('')
p.set_ylabel('')

# plt.subplots_adjust(left=0.2)
fig.suptitle('Überblick Akkuratheit Algorithmen (n = 57)', fontsize=16)
fig.set_size_inches(8.27,11.69)
plt.subplots_adjust(left=0.2, right=0.95, bottom=0.05)
fig.savefig('accuracy-overview.png', format='png')
# plt.show()




###############################
# Compare good algs in detail #
###############################

# filter out boring, bad algorithms
sorter = [
    'PPG',
    'MalvarHeCutler',
    'HAMILTON_ADAMS',

    'RCD',

    'DLMMSE_CODE',
    'DLMMSE_PAPER',

    'DLMMSE_RCD',
    'DLMMSE_RCD_PAPER'
]
sorter.reverse()
results = results[results.alg.isin(sorter)]
results.alg = results.alg.astype("category")
results.alg = results.alg.cat.set_categories(sorter)

#######
# MSE #
#######
# fig, ((p1, p2), (p3, p4)) = plt.subplots(2, 2)
fig, ((p1), (p2), (p3), (p4)) = plt.subplots(4, 1)
p = results.boxplot(column=['mse'], by=['alg'], vert=False, ax=p1)
p.set_title('Gesamt (n=57)')
p.set_xlabel('')
p.set_ylabel('')


kodak = results[results.dataset == 'kodak']
p = kodak.boxplot(column=['mse'], by=['alg'], vert=False, ax=p2)
p.set_title('Kodak (n=24)')
p.set_xlabel('')
p.set_ylabel('')
# p.set(yticklabels=[])


mcmaster = results[results.dataset == 'mcmaster']
p = mcmaster.boxplot(column=['mse'], by=['alg'], vert=False, ax=p3)
p.set_title('McMaster (n=18)')
p.set_xlabel('')
p.set_ylabel('')


cambridge = results[results.dataset == 'cambridge']
p = cambridge.boxplot(column=['mse'], by=['alg'], vert=False, ax=p4)
p.set_title('Cambridge (n=15)')
p.set_xlabel('')
p.set_ylabel('')
# p.set(yticklabels=[])

# plt.subplots_adjust(left=0.2)
fig.suptitle('Mean Square Error', fontsize=16)
fig.set_size_inches(8.27,11.69)
plt.subplots_adjust(left=0.2, right=0.95, bottom=0.05, hspace=0.3)
fig.savefig('accuracy-mse.png', format='png')
# plt.show()

########
# PSNR #
########
# fig, ((p1, p2), (p3, p4)) = plt.subplots(2, 2)
fig, ((p1), (p2), (p3), (p4)) = plt.subplots(4, 1)

p = results.boxplot(column=['psnr'], by=['alg'], vert=False, ax=p1)
p.set_title('Gesamt (n=57)')
p.set_xlabel('')
p.set_ylabel('')


kodak = results[results.dataset == 'kodak']
p = kodak.boxplot(column=['psnr'], by=['alg'], vert=False, ax=p2)
p.set_title('Kodak (n=24)')
p.set_xlabel('')
p.set_ylabel('')
# p.set(yticklabels=[])

mcmaster = results[results.dataset == 'mcmaster']
p = mcmaster.boxplot(column=['psnr'], by=['alg'], vert=False, ax=p3)
p.set_title('McMaster (n=18)')
p.set_xlabel('')
p.set_ylabel('')


cambridge = results[results.dataset == 'cambridge']
p = cambridge.boxplot(column=['psnr'], by=['alg'], vert=False, ax=p4)
p.set_title('Cambridge (n=15)')
p.set_xlabel('')
p.set_ylabel('')
# p.set(yticklabels=[])

# plt.subplots_adjust(left=0.2)
fig.suptitle('Peak Signal to Noise Ratio', fontsize=16)
fig.set_size_inches(8.27,11.69)
plt.subplots_adjust(left=0.2, right=0.95, bottom=0.05, hspace=0.3)
fig.savefig('accuracy-psnr.png', format='png')
# plt.show()

#########
# MSSIM #
#########
# fig, ((p1, p2), (p3, p4)) = plt.subplots(2, 2)
fig, ((p1), (p2), (p3), (p4)) = plt.subplots(4, 1)

p = results.boxplot(column=['mssim'], by=['alg'], vert=False, ax=p1)
p.set_title('Gesamt (n=57)')
p.set_xlabel('')
p.set_ylabel('')


kodak = results[results.dataset == 'kodak']
p = kodak.boxplot(column=['mssim'], by=['alg'], vert=False, ax=p2)
p.set_title('Kodak (n=24)')
p.set_xlabel('')
p.set_ylabel('')
# p.set(yticklabels=[])

mcmaster = results[results.dataset == 'mcmaster']
p = mcmaster.boxplot(column=['mssim'], by=['alg'], vert=False, ax=p3)
p.set_title('McMaster (n=18)')
p.set_xlabel('')
p.set_ylabel('')


cambridge = results[results.dataset == 'cambridge']
p = cambridge.boxplot(column=['mssim'], by=['alg'], vert=False, ax=p4)
p.set_title('Cambridge (n=15)')
p.set_xlabel('')
p.set_ylabel('')
# p.set(yticklabels=[])

# plt.subplots_adjust(left=0.2)
fig.suptitle('Mean Structural Similarity Index Measure', fontsize=16)
fig.set_size_inches(8.27,11.69)
plt.subplots_adjust(left=0.2, right=0.95, bottom=0.05, hspace=0.3)
fig.savefig('accuracy-mssim.png', format='png')
plt.show()