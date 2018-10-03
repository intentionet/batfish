
**Got questions, feedback, or feature requests? Join our community on [Slack!](https://join.slack.com/t/batfish-org/shared_invite/enQtMzA0Nzg2OTAzNzQ1LTUxOTJlY2YyNTVlNGQ3MTJkOTIwZTU2YjY3YzRjZWFiYzE4ODE5ODZiNjA4NGI5NTJhZmU2ZTllOTMwZDhjMzA)**

[![codecov](https://codecov.io/gh/batfish/batfish/branch/master/graph/badge.svg)](https://codecov.io/gh/batfish/batfish)

## What is Batfish?

Batfish is a network configuration analysis tool that can find bugs and guarantee the correctness of (planned or current) network configurations. It enables network engineers to rapidly and safely evolve their network, without fear of outages or security breaches.

**Batfish does not require access to network devices.** It only needs offline configuration files, e.g., pulled from RANCID or template authoring tools.

The developers of Batfish include folks from [Intentionet](https://www.intentionet.com), University of California Los Angeles, University of Southern California, and Microsoft Research. See [www.batfish.org](http://www.batfish.org) for technical information on how it works. ![Analytics](https://ga-beacon.appspot.com/UA-100596389-3/open-source/batfish?pixel&useReferer)

## How do I get started?

For a quick start, check out instructions for using the pre-built [allinone Docker container](https://github.com/batfish/docker/blob/master/allinone.md) (Batfish plus a Python client) or the [Batfish-only Docker container](https://github.com/batfish/docker/blob/master/batfish.md), available on Docker Hub.

If you are interested in developing or building Batfish yourself, go to [Batfish Wiki](https://github.com/batfish/batfish/wiki).

Batfish is being maintained and released as a commercial product by [Intentionet](https://www.intentionet.com). If you'd like a hosted service, drop a line to [info@intentionet.com](mailto:info@intentionet.com).

## What kinds of correctness checks does Batfish support?

Batfish supports many types of correctness checks, including

1. ##### Compliance and best-practices guidelines, e.g.:
  - Flag undefined-but-referenced or defined-but-unreferenced structures (e.g., ACLs, route maps)
  - Ensure that all interface MTUs are per the network's standard
  - AAA, SNMP, and NTP configuration is correct
  - Devices can only be accessed using SSHv2 and password is not null
  - Logging is on

2. ##### Consistency of configuration across two or more devices, e.g.:
  - BGP sessions are compatibly configured across neighbors
  - IPSec/VPN tunnels compatibly configured with the same key
  - All interface IP addresses are unique
  - Identically-named structures (e.g., ACLs, route maps) across devices have identical functionality

3. ##### Checks on data flow, e.g.:
  - Path (shape) between two devices is as expected (e.g., traverses a firewall, valley-free routing)
  - Number of paths between two devices is as expected (i.e., correct multi-path configuration)
  - Paths for two devices inside the data center never leaves the data center
  - Certain services (e.g., DNS) are globally reachable and not blocked by any ACL in the network
  - Certain sensitive services can be reached only from specific subnets or devices
  - All pairs of top-of-rack switches can reach each other

4. ##### Fault-tolerance, e.g.:
  - End-to-end reachability is not impacted for *any* flow after *any* single-link or -device failure
  - Traffic correctly fails over after a failure

5. ##### "Differential" analysis of two sets of configuration, e.g.:
  - End-to-end reachability is identical across new and old configurations
  - Planned ACL changes have no collateral damage, e.g., relative to the current configuration, only flows that are intended to be (un)blocked are (un)blocked; no other flow is impacted.
  - Two configurations, potentially from different vendors, are semantically equivalent

These checks are performed by asking questions about configurations. See [here](https://github.com/batfish/batfish/wiki/Questions) for the list of questions.

## What configuration formats does Batfish support?

Batfish supports configurations for a large and growing set of (physical and virtual) devices, including:
  - Arista
  - Aruba
  - AWS VPCs
  - Cisco (All Cisco NX-OS, IOS, IOS-XR, IOS-XR devices including Cisco ASA)
  - Dell Force10
  - Foundry
  - iptables (on hosts)
  - Juniper (All JunOS plaforms: MX, EX, QFX, SRX)
  - MRV
  - Palo Alto Networks
  - Quagga
  - Quanta
  - VyOS

If you'd like support for additional vendors or currently-unsupported configuration features, file feature requests or bug reports using the [GitHub issue tracker](https://github.com/batfish/batfish/issues/new)
and we'll try to fix. Or, you can -- we welcome pull requests! :)
