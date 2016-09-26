# *Eircode* and *postcode* proxy service

## Background

Eircode is Ireland's first public database of unique identifiers for all addresses. Introduced by the Irish government in July 2015, it is intended to allow citizens, businesses and public bodies to locate every individual address in the country. The registries of that information are the Eircode Address Database (ECAD) and the Eircode Address File (ECAF).

UK has its own postcode, maintained by the Royal Mail.

## Problem

Information to both data bases can be retrieved from third party services, as [Allies Computing](https://www.alliescomputing.com/), but the service can be expensive.

## Requirements

### Core requirements

Because of the cost of the external service, and in order to minimise network traffic as regards information that changes so slowly, a proxy service has been devised, capable of caching the queries and results. A service that:

- Exposes an API that is compatible with and uses the third-party API.
- Avoids repeated requests to hit the third party API.

### Secondary requirements

This service can be called by multiple services, that can add up to one million requests per month. Thus, the service:

- Makes sure the previous requests survive on service restarts.
- Is easy (or automatic) to scale horizontally, to cope with the load.

