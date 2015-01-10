Cropper
=======

Quickly cut features out of images.  Use green paint for the parts you want; use red for the parts you don't.


The UI is made in [NetBeans IDE](https://netbeans.org), so I suggest running and building with NetBeans.

## Future plans

Java is not a good platform for the web, even with WebStart packaging.  Cropper will be most useful if it is ported to JavaScript and run in the browser.  I am no longer supporting this Java version.  If any ambitious person wants to take on the project, feel free to fork!

## Known issues

* Does not run on Windows; I have no idea why
* Anti-aliasing on "hardest" edge setting is still a bit soft
* Local debugging is difficult (javax.jnlp.FileOpenService import causes ClassNotFoundException)

## License

Copyright (c) 2014 Neal Ehardt. This software is licensed under the MIT License.

Different licenses may apply to other software that this project depends on, please check [LICENSE.md](https://github.com/NealEhardt/cropper/blob/master/LICENSE.md) for their licenses.
