# spring-session-cassandra #

Support for using Cassandra as HTTP session storage for spring-boot.

## Usage ##

### 1. Add the dependency to your project ###

```xml
<dependency>
    <groupId>org.springframework.session.data.cassandra</groupId>
    <artifactId>spring-session-cassandra</artifactId>
    <version>0.1</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/src/main/resources/lib/spring-session-cassandra-0.1.jar</systemPath>
</dependency>
```
### 2. Add the following configuration to your application.properties file ###

```properties

To use this module you just need to follow these steps:
* Configure a Cassandra database using the configuration for spring-data-cassandra. Example:
``` properties
##########################################################################
################# DB Config ##############################################
##########################################################################
spring.data.cassandra.contact-points=192.168.0.1
spring.data.cassandra.keyspace-name=main
spring.data.cassandra.local-datacenter=dc1
spring.data.cassandra.username=dbadmin
spring.data.cassandra.password=password
spring.data.cassandra.port=9042
spring.data.cassandra.schema-action=NONE
spring.data.cassandra.request.consistency=local-quorum
spring.data.cassandra.request.serial-consistency=local-quorum
```

### 3. Create the following tables in your Cassandra database ###

```sql
CREATE TABLE spring_session (
    id uuid PRIMARY KEY,
    attributes map<text, text>,
    creation_time TIMESTAMP,
    last_accessed TIMESTAMP,
    max_inactive_interval_in_seconds int
);

CREATE TABLE spring_session_by_name (
    principal_name text,
    id uuid,
    PRIMARY KEY (principal_name, id)
)
```


## Dependencies ##
This module depends on the following modules:
* spring-boot-starter-parent (Tested on version 2.7.11.RELEASE)
* spring-boot-starter-web (Tested)
* spring-boot-starter-data-cassandra (Tested)
* spring-session-core (Tested on version 2.7.1.RELEASE)

