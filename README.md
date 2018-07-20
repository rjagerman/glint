# [![Glint](https://github.com/rjagerman/glint/wiki/images/glint-logo-small.png "Glint")](https://github.com/rjagerman/glint)
[![Build Status](https://travis-ci.org/rjagerman/glint.svg?branch=add-ci-testing)](https://travis-ci.org/rjagerman/glint)

Glint is a high performance [Scala](http://www.scala-lang.org/) parameter server built using [Akka](http://akka.io/).
The aim is to make it easy to develop performant distributed machine learning algorithms using the parameter server architecture. One of the major goals is compatibility with [Spark](http://spark.apache.org/).

## Compile
To use the current version you should compile the system manually, publish it to a local repository and include it in your project through sbt. Clone this repository and run:

    sbt "+ compile" "+ assembly" "+ publish-local"

The `+` indicates that it should compile for all scala versions defined in the `build.sbt` file. The command will compile, assemble and publish the library jar file to the local ivy2 repository, which means you can then use it in your project's `build.sbt` (on the same machine) as follows:

    libraryDependencies += "ch.ethz.inf.da" %% "glint" % "0.1-SNAPSHOT"

## Documentation

Refer to the [documentation](http://rjagerman.github.io/glint/) for instructions and examples on how to use the software.

## Citation

If you find this project useful in your research, please cite the following paper in your publication(s):

Rolf Jagerman, Carsten Eickhoff and Maarten de Rijke. **"Computing Web-scale Topic Models using an Asynchronous Parameter Server."** *(2017)*

    @inproceedings{jagerman2017computing,
      title={Computing Web-scale Topic Models using an Asynchronous Parameter Server},
      author={Jagerman, Rolf and Eickhoff, Carsten and de Rijke, Maarten},
      booktitle={Proceedings of the 40th International ACM SIGIR Conference on Research and Development in Information Retrieval},
      year={2017},
      organization={ACM}
    }
