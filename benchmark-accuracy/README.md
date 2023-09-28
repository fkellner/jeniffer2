# Benchmarking Setup for Demosaicing Algorithms

To perform the image-quality benchmarking in this folder, you need to have GNU Make 
as well as other dependencies outlined in `dlmmse/readme.md` installed.

- `make tools` compiles the C code in `dlmmse`, including the `mosaic` and `imdiff` tools
- `make mosaic` creates undemosaiced versions of the images in `truth`. However, since these cannot be automatically converted to 16bit PNG via ImageMagick (`-depth 16` being ignored) and have to be manually converted using GIMP, the undemosaiced versions are committed to vcs, too

## Tools

For creating the mosaiced images and evaluating image differences,
the `mosaic` and `imdiff` tools from the DLMMSE implementation by 
Pascal Getreuer are used (folder: DLMMSE). 
Although in contrast to JENIFFER2, `imdiff` can only handly 8 bit bit depth,
the images from the Kodak and McMaster data sets also only have 8 bit depth,
so it should be fine.

For compilation instructions and licensing details, see `dlmmse/readme.md`.

## Image Data Sets

This folder contains the images used for benchmarking demosaicing algorithms
by Andreas Reiter in 2023 (see `papers` folder TODO). 

Sources:

- [Kodak](https://r0k.us/graphics/kodak/index.html)
- [McMaster_IMAX](https://www4.comp.polyu.edu.hk/~cslzhang/CDM_Dataset.htm)

The McMaster Dataset has been converted from `.tif` to `.png` using the [ImageMagick Convert Script](https://www.imagemagick.org/script/convert.php),
which has a high chance of already being installed on common linux distributions,
in order to simplify using them in the benchmark and save storage space. 

Reiter also used the Panasonic Images 1-14 and 500 and their noisy versions
from the [Microsoft Research Cambridge Demosaicing Dataset](https://www.microsoft.com/en-us/download/details.aspx?id=52535) *Note that this is the link to the entire dataset, consisting of over 2GB of images*.
Since the license of this dataset does not allow redistribution, it is not included here.
