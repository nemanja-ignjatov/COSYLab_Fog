# COSYLab Fog components

#### General notice

--------------------------------------

This project is developed within the COSYLab IoT framework for Smart Homes, in the scope of the dissertation "Trustworthy Context-Aware
Access Control in IoT Environments based on the Fog Computing Paradigm".
Currently, the design details for the COSYLab are provided through blogs published on the netidee.at website.

Therefore, please visit the following web page for more details on COSYLab:

https://www.netidee.at/trustworthy-context-aware-access-control-iot-environments-based-fog-computing-paradigm

--------------------------------------

### Components

This repository contains Fog components developed within the COSYLab framework, 
offering service for trustworthy networking, access control, and context-awareness.
Features supported by the components are as follows:
- Fog_FACA (Fog Access Control Agent) - access control and user management services based on ABAC;
- Fog_FTA (Fog Trust Anchor) - root PKI node for the Fog components, offering PKI services for the communication 
between Fog components and also with Cloud components available in https://github.com/nemanja-ignjatov/COSYLab_Cloud;
- Fog_FTP (Fog Trust Provider) - PKI node that enables offloading PKI operations away from IoT devices;
- Fog_CCAA (Connectivity Context-Awareness Agent) - Tool for monitoring the connectivity to the Cloud components and 
notifying current connection state to the FACA.

### Dependencies

Developed component are based on following technologies:
- Java 11
- Maven
- Spring Boot
- MongoDB
- RabbitMQ

### Build

Before building Fog component, COSYLab utilities have to be compiled. To achieve that,
please follow instruction provided in https://github.com/nemanja-ignjatov/COSYLab_Utils.

Once COSYLab utilities are built and installed, each Fog component can be compiled.
This is done by positioning in the directory of the particular component and executing:
> mvn clean install

### Configuration

Once built, each component needs to be configured before its execution.
This is achieved by 
- (1) editing application.properties file in the src/main/resources
folder of the given Fog component by assigning the values to the properties
that currently have ${} as part of their value or 
- (2) setting variables values
in the execution environment (e.g., as operating system variables) for the 
variable names surrounded with ${}.

Once the configuration properties are set, certificate of the TNTA component needs to be
configured by setting the certificate in PEM format in tntacert.pem file.

### Execution

Once configured, Fog components can be started as Java applications.
Starting a Fog component is done by positioning in the component's directory and executing:
> java -jar target/${executable_jar_filename}.jar

For example:
> java-jar target/fog_trust_provder-1.2.jar

### Docker

Beside standalone application execution, Fog components can be started in Docker containers.
For that, Docker image needs to be build using command executed in the given component's directory:
>docker build -t ${fog_components_image_name} .

For example:
>docker build -t ignjatov90/fog_ctrl .

