# Ballerina WebSockets Test Examples

## Overview

This is a set of Ballerina WebSocket test examples. The examples demonstrates how to use the AsyncAPI tools to generate the specification from a Ballerina WebSocket service and how to use the generated specification to create a Ballerina WebSocket client. Moreover, the examples demonstrate how to use the generated WebSockets client to write a simple client application.

## Generating the AsyncAPI specification

You can generate a demo AsyncAPI specification using the `server-demo` package. A pregenerated specification already exists in the `specs` directory. If you want to generate the specification again, you can use the following command, where `main.bal` is the Ballerina file that contains the entry point to the WebSocket service.

```bash
cd server-demo
bal asyncapi --protocol ws -i main.bal -o specs
```

## A client-server example

The `client` and `server` packages exemplify a basic client-server application. The client sends a message to the server, which responds with a message, and the client logs the response. The client continuously listens for messages from the server.

You can have two instances of the client to pass messages between them through the server.

In the `client` package, the files `client.bal`, `types.bal`, and `utils.bal` are generated by the AsyncAPI tool, whereas the `main.bal` file is an example usage of the generated client.

In the `server` package, the `messages.md` file provides example messages outlining the expected structure by the server. The `main.bal` file contains the WebSocket server implementation.

You can regenerate the client using the following command:

```bash
cd client
bal asyncapi --protocol ws -i asyncapi.yaml
```

You can run the server and client using the following commands:

```bash
bal run server
bal run client
```

## A client-server example with a dispatcher stream id

The same application can be run with a dispatcher stream id using the `client-with-dispatcherStreamId` package and the `server-with-dispatcherStreamId` package.