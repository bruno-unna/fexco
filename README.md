# *Eircode* and *postcode* proxy service

TL;DR
- Instructions for building and running the exercise are provided 
[at the end](#how_to_run).
- This project builds two docker images:
  - One for development purposes, that generates random responses 
    (module `mock-service`).
  - Another (the important one) that is the required development 
    (module `proxy-service`).
- A Redis server is required.
- A docker-compose file for the wrapped-up final product is included, that 
  takes care of all servers and configuration.

## Background

Eircode is Ireland's first public database of unique identifiers for all 
addresses. Introduced by the Irish government in July 2015, it is intended 
to allow citizens, businesses and public bodies to locate every individual 
address in the country. The registries of that information are the Eircode 
Address Database (ECAD) and the Eircode Address File (ECAF).

UK has its own postcode, maintained by the Royal Mail.

## Problem

Information to both data bases can be retrieved from third party services, 
as [Allies Computing](https://www.alliescomputing.com/), but the service 
can be expensive.

### Requirements

#### Core requirements

Because of the cost of the external service, and in order to minimise 
network traffic as regards information that changes so slowly, a proxy 
service has been devised, capable of caching the queries and results. A 
service that:

- Exposes an API that is compatible with and uses the third-party API.
- Avoids repeated requests to hit the third party API.

#### Secondary requirements

This service can be called by multiple services, that can add up to one 
million requests per month. Thus, the service:

- Makes sure the previous requests survive on service restarts.
- Is easy (or automatic) to scale horizontally, to cope with the load.
- Must take the form of Docker images.

## Solution

The solution is correctly outlined in the requirements, but these are 
overlooking some aspects of modern distributed systems, aspects that are 
worth bringing forward in this solution.

A modern solution, in order to compete and be useful under the ever more 
stressing demands of current distributed systems, should be responsive, 
resilient and elastic. More often than not, these desired features are 
achievable by means of event-based architectures.

### Reactivity

The aforementioned features of a modern architecture have been described 
in the [Reactive Manifesto](http://www.reactivemanifesto.org/). Several 
approaches have emerged to its fulfillment, with two outstanding schools: 
reactive extensions (Rx) and reactive streams (RS). Each of them have 
implementations in the form of libraries, toolboxes or frameworks. Some 
implementations offer the two approaches at the same time.

A key concept of the reactive way is called *back pressure propagation*. 
It refers to the up-stream signaling (from consumers to producers) for 
the control of information traffic. This way, the traditional problems 
of consumer starvation and -more importantly- of buffer overflows are 
efficiently solved. A common trait of reactive technologies is the 
inclusion of automatic back pressure propagation in distributed systems.

### Architecture

The overall architecture of this solution is based on micro-services, 
but in addition to that, reactivity has been considered in the design. 
As per the requirements, a Docker image is created, running a Vert.x 
service (only a http RESTful interface, and only a GET method).

In order to achieve persistence for the cache, a Redis server is used. 
This provides the cache server with an high-efficiency replicated 
in-memory cache, providing at the same time the benefits of replication 
(for high-availability) and durability (via redis configuration).

```
+----------+             +--------+
| external |             | proxy  |             +-------+
| server   | <-- http -- | server | <-- tcp --> | REDIS | ...
|          |             |        | ...         +-------+
+----------+             +--------+
                              ^
                              |
                              |
                         +----------+
                         | internal |
                         | consumer | ...
                         +----------+
```

This diagram shows the components of the solution. It can be seen 
ellipsis next to several boxes:

- The internal consumers can be numerous, by the definition of the problem.
- Redis servers can be deployed in high-availability mode (clustered).
- The proxy server, which is the main matter of this exercise, can be 
  deployed after a load balancer if the load becomes really heavy.

### Technology stack

The solution is flexible enough to be run in any environment as long as 
it includes a Redis server (or cluster). In particular, a Docker 
environment is going to be used, according to specifications. For that 
reason, the Redis server image has been included in the provided 
[docker-compose](https://docs.docker.com/compose/overview/) 
specification.

For storing the information a [Redis](http://redis.io/) server was 
preferred over an ad-hoc in-memory handling/storage/retrieval, 
because it provides the speed of in-memory storage, the convenience 
of distributed information, the required persistence and the 
high-availability clustering if needed. Besides, reactive drivers 
exist for Redis in many reactive frameworks, including Vert.x.

Since the solution was devised as a reactive application, a stack 
was chosen, as simple as possible but fit to purpose. Based on that 
reasoning, and given recent familiarisation with it, 
[Vert.x](http://vertx.io/) was selected.

### Discussion

#### Code structure

TBD

#### <a name="how_to_run"></a>How to run

1. Make sure you have the following tools available:
    - A Java 8 JDK.
    - Maven builder (tested with version 3.3.9).
    - A `docker` client available (tested with version 1.12.1).
    - `docker-compose` (tested with version 1.8.0).
1. Clone [this repository](https://github.com/bruno-unna/fexco).
1. Within the base directory of the project, run `mvn package`.
1. Run `docker-compose -p fexco up`.
1. Connect using a browser to [the proxy server](http://localhost:8080/pcw/PCW45-12345-12345-1234X/address/ie/D02X285).
1. Play changing the values in the URL and observe the cache hits and misses in the console.

#### Next steps and enhancements

- Adding a TTL (time to live) to the cached information. This is trivial 
  when using Redis but needs discussion and needs to be configured 
  in the solution.
- Writing integration tests. Several libraries exist to help with 
  this, AssertJ and Rest-Assured are known to play nicely with Vert.x. 
  It is recognised that this should have been done beforehand, but 
  time constraints forced the lowering of its priority.
- Providing ex-ante thorough unit tests. It is generally preferred 
  to write tests first (TDD), specially in complex or multi-component 
  systems. Again, time constraints limited very much the amount 
  of tests that were automated in the solution.
- Soak test the solution, hammering and stressing it to discover 
  how well it can potentially scale. JMeter can be used for this.