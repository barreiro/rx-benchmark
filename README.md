# Hibernate Reactive Benchmark 🚧 🚧 🚧

This benchmark aims to compare Hibernate Reactive performance against plain Hibernate ORM.

It's initially based on the examples in https://github.com/hibernate/hibernate-reactive/tree/master/example

## Instructions

### Setup database

Start a postgeSQL instance using podman:

```podman run -it --rm=true --name HibernateTestingPGSQL -e POSTGRES_USER=hreact -e POSTGRES_PASSWORD=hreact -e POSTGRES_DB=hreact -p 5432:5432 postgres:12```

### Run benchmark

```./gradlew jmh```

Adjust JMH configuration in build.gradle. 
See https://github.com/melix/jmh-gradle-plugin/blob/master/README.adoc#configuration-options for further JMH configurations

The benchmark runs constricted to a single CPU core. This can be changed in constrainedJVM.sh. 

### Results

The results are available in build/reports/jmh folder.