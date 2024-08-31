Im trying to create a dynamic proxy system which could join your home network to the open web through a thin proxy server. Vision is to "make" that proxy server dynamically customizable to route traffic to your home network.

Im primarily using IntelliJ build system for JARS.
Its still kinda in progress.

To run client:

```
java -cp client.jar space.themelon.openspace.client.Main <serverip>:<serverport>
```

To run server:

```
java -cp server.jar space.themelon.openspace.server.Main <host_port> <proxy_port> <toml>
```

Toml file contains allowed network list:

```toml
addresses = [
    # addr_type = "ipv4", "ipv6", "domain"
    # [addr_type, addr, [allowed_port_range]]

    ["192.168.0.103", "1024", "1024"]
]
```