 +-----------------------------------------------------------------------------------------------------+
 |                             National Institute of Standards and Technology                          |
 |                                        Biometric Clients Lab                                        |
 +-----------------------------------------------------------------------------------------------------+
  File author(s):
       Kevin Mangold (kevin.mangold@nist.gov)
       Jacob Glueck (jacob.glueck@nist.gov)

 +-----------------------------------------------------------------------------------------------------+
 | NOTICE & DISCLAIMER                                                                                 |
 |                                                                                                     |
 | The research software provided on this web site ("software") is provided by NIST as a public        |
 | service. You may use, copy and distribute copies of the software in any medium, provided that you   |
 | keep intact this entire notice. You may improve, modify and create derivative works of the software |
 | or any portion of the software, and you may copy and distribute such modifications or works.        |
 | Modified works should carry a notice stating that you changed the software and should note the date |
 | and nature of any such change.  Please explicitly acknowledge the National Institute of Standards   |
 | and Technology as the source of the software.                                                       |
 |                                                                                                     |
 | The software is expressly provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED,  |
 | IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF      |
 | MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY.  NIST        |
 | NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR         |
 | ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED.  NIST DOES NOT WARRANT OR MAKE ANY               |
 | REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED |
 | TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.                           |
 |                                                                                                     |
 | You are solely responsible for determining the appropriateness of using and distributing the        |
 | software and you assume all risks associated with its use, including but not limited to the risks   |
 | and costs of program errors, compliance with applicable laws, damage to or loss of data, programs   |
 | or equipment, and the unavailability or interruption of operation.  This software is not intended   |
 | to be used in any situation where a failure could cause risk of injury or damage to property.  The  |
 | software was developed by NIST employees.  NIST employee contributions are not subject to copyright |
 | protection within the United States.                                                                |
 |                                                                                                     |
 | Specific hardware and software products identified in this open source project were used in order   |
 | to perform technology transfer and collaboration. In no case does such identification imply         |
 | recommendation or endorsement by the National Institute of Standards and Technology, nor            |
 | does it imply that the products and equipment identified are necessarily the best available for the |
 | purpose.                                                                                            |
 +-----------------------------------------------------------------------------------------------------+

 This software was developed using multiple operating systems and configurations using Java 1.6/1.7
 (see below for the list). Apache Maven was used as the authoritative build tool.  Testing was also
 performed with Eclipse's built-in build process. Apache Jersey 1.9.1 was used to enable easy standalone
 web hosting. Third party libraries are not distributed with this package.


 +-----------------------------------------------------------------------------------------------------+
 | Tested Environments                                                                                 |
 +-----------------------------------------------------------------------------------------------------+

    Windows 8 Enterprise
        Processor: 2.53 GHz Xeon E5540 64-bit
        RAM: 48 GB
        Java version: 1.6.0_45 and 1.7.0_12
        Maven version: 2.2.1

    CentOS 6.2, 6.3, 6.4
        Processor: 2.53 GHz Virtual Processor (HyperV) 32-bit
        RAM: 2 GB
        Java version: 1.6.0_39 and 1.7.0_13 and 1.7.0_45
        Maven version: 2.2.1 and 3.0.4

    Ubuntu 10.04
        Processor: 2.27 GHz Intel Xeon E5520 64-bit
        RAM: 1024 MB
        Java version: 1.6.0_26
        Maven version: 2.2.1


 +-----------------------------------------------------------------------------------------------------+
 | Project Hierarchy                                                                                   |
 +-----------------------------------------------------------------------------------------------------+

    pom.xml                       * the root/parent POM for this WS-BD distribution

    wsbd-core                     * all core WS-BD source code and related files
        pom.xml                       - the POM for the 'wsbd-core' project
        src/main/xsd/schema.xsd       - schema file associated with types defined in the specification
        src                           - the source code for the 'wsbd-core' project

    wsbd-example                  * an example service which generates cool pictures as sensor data
        pom.xml                       - the POM for the 'wsbd-core' project
        src                           - the source code for the 'wsbd-example' project

		
 +-----------------------------------------------------------------------------------------------------+
 | Dependencies                                                                                        |
 +-----------------------------------------------------------------------------------------------------+

    - Oracle Java 1.7.0 or later
    - Apache Jersey 2.2 (and related dependencies)
    - Apache Maven 2.2.1, 3.0.3, or 3.0.4 (and related dependencies)
    - JUnit 4.11 (and related dependencies) (to run unit tests)


 +-----------------------------------------------------------------------------------------------------+
 | Building & Installation                                                                             |
 +-----------------------------------------------------------------------------------------------------+

 To build the source code, verify that appropriate versions of Java, Maven, and Subversion are
 installed and configured. In this case, 'configured' means globally accessible through the command
 line. Navigate to %INSTALL_DIRECTORY%/wsbd-core (%INSTALL_DIRECTORY% is the location of the
 uncompressed package). Enter the following command:

        mvn clean install

 After this process is finished, there will be an automatically generated directory named 'target'.
 Inside the 'target' directory, a JAR file named 'wsbd-core-1.2.jar' will exist. Include this JAR in
 the build path for your WS-BD projects.

 
 +-----------------------------------------------------------------------------------------------------+
 | Documentation                                                                                       |
 +-----------------------------------------------------------------------------------------------------+

 Build the java doc using:

        mvn javadoc:javadoc

 The javadoc will be in the folder  wsbd-core/target/site/apidocs/index.html
 
 
 +-----------------------------------------------------------------------------------------------------+
 | Running Example Service                                                                             |
 +-----------------------------------------------------------------------------------------------------+

 After the projects have been built (see Building & Installation), open a command prompt/terminal and
 navigate to the "<ROOT_POM_PATH>/wsbd-example" directory. Enter the following command:

     mvn exec:java

 The server will print out an explanation of how to use it. It will also provide a live stream as a
 demonstration. To view the stream, open "<ROOT_POM_PATH>/wsbd-example/streamview.html" in a web
 browser.

 During the package phase, a runnable JAR will also be built at
 "wsbd-example/target/wsbd-example-1.0-jar-with-dependencies.jar". You can also run that:

     java -jar wsbd-example-1.0-jar-with-dependencies.jar

 The example service contains only 3 classes, one of which just generates images. The main class is
 SimulatedServer, which uses a WSBDServer and a SimulatedSensorService to create a working service.

 
 +-----------------------------------------------------------------------------------------------------+
 | Comments, Suggestions, and Contributions                                                            |
 +-----------------------------------------------------------------------------------------------------+

 We would love to hear how others are using WS-Biometric Devices and/or the NIST reference
 implementation. Please submit all questions, comments, suggestions, or issues to <wsbd@nist.gov>.
