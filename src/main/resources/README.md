util-base
=========

	
	Copyright 2013-2017 Hauser Olsson GmbH 
	Copyright 2018 Jean-Paul Vetterli
 	
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
    	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

***

Util-base is a smallish collection of utility classes useful in many 
applications. It provides, or standardizes access to, functions like

- managing messages and strings in property resources;
- defining and accessing application configuration parameters;
- logging;
- inversion of control/dependency injection.
 
The functions are explained in detail in the javadoc.

For Maven users
---------------

Starting with version 3.0.0, the software is available from the 
[Maven central repository](http://repo.maven.apache.org/maven2/ch/agent/util-base/).
To use version x.y.z, insert the following dependency into your `pom.xml` file:

    <dependency>
      <groupId>ch.agent</groupId>
      <artifactId>util-base</artifactId>
      <version>x.y.z</version>
      <scope>compile</scope>
    </dependency>

Building the software
---------------------

The recommended way is to use [git](http://git-scm.com) for accessing the
source and [maven](<http://maven.apache.org/>) for building. The procedure 
is easy, as maven takes care of locating and downloading dependencies:

	$ git clone https://github.com/jpvetterli/util-base.git
	$ cd util-base
	$ mvn install

This builds and installs the distribution JARs in your local maven
repository. They can also be found in the `target` directory.

Browsing the source code
------------------------

The source is available on [GitHub](http://github.com/jpvetterli/util-base.git).

