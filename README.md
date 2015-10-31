# android-Multipicker
Pick multiple photos/videos. Media are shown in custom gallery and separated into folders.

Multipicker searches through all files for particular media type - images or videos. These
are sorted into folders and shown in a gallery. The user may navigate through folders and
select arbitrary set of images or videos. This selection is then returned as a result of
a whole activity.

The media files are loaded asynchronously using Loaders. To display their
thumbnails, this library uses [Universal Image Loader](https://github.com/nostra13/Android-Universal-Image-Loader) 
library.