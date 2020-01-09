/*----------------------------------------------------------------------------------------------------+
 |                             National Institute of Standards and Technology                          |
 |                                        Biometric Clients Lab                                        |
 +-----------------------------------------------------------------------------------------------------+
  File author(s):
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
 +----------------------------------------------------------------------------------------------------*/

/**
 * Contains: classes related to configuration of the service.
 *
 * The service configuration is divided into two parts: the server configuration
 * and the service configuration. The server configuration is all read only and
 * describes things like the maximum number of concurrent sessions and the
 * timeouts for various operations. The sensor configuration may contain both
 * read only and writable parameters which describe the sensor such as modality,
 * submodality, and various sensor-specific parameters. For each type of
 * configuration class
 * ({@link gov.nist.itl.wsbd.configuration.ServerConfiguration} and
 * {@link gov.nist.itl.wsbd.configuration.SensorConfiguration}), there is a
 * corresponding information class
 * (@{@link gov.nist.itl.wsbd.configuration.ServerInformation} and
 * {@link gov.nist.itl.wsbd.configuration.SensorInformation}} which describes
 * the allowed values, default values, and other properties of the
 * configuration. Finally, special configuration values are in
 * {@link gov.nist.itl.wsbd.configuration.ServerStateKey}. These represent
 * special read only values which the server changes, like the last updated time
 * and the sensor status. The information and configuration classes are
 * {@link gov.nist.itl.wsbd.configuration.ServerStateInformation} and
 * {@link gov.nist.itl.wsbd.configuration.ServerStateConfiguration},
 * respectively.
 *
 * The class {@link gov.nist.itl.wsbd.configuration.ServiceConfiguration}
 * contains all four of the previous classes and represents the entire service
 * configuration.
 *
 * @author Jacob Glueck
 *
 */
package gov.nist.itl.wsbd.configuration;