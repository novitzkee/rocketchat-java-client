---
name: ddp-protocol
description: Explains core concepts of DDP protocol. Use when detailed understanding of DDP protocol is required.
author: Meteor.js Developers
version: 1.0
---

# DDP Specification

DDP is a protocol between a client and a server that supports two operations:

* Remote procedure calls by the client to the server.
* The client subscribing to a set of documents, and the server keeping the
  client informed about the contents of those documents as they change over
  time.

This document specifies the version "1" of DDP. It's a rough description of
the protocol and not intended to be entirely definitive.

## General Message Structure:

DDP messages are JSON objects, with some fields specified to be EJSON.  Each one
has a `msg` field that specifies the message type, as well as other fields
depending on message type.

The client and the server must ignore any unknown fields in messages.  Future
minor revisions of DDP might add extra fields without changing the DDP version;
the client must therefore silently ignore unknown fields.  However, the client
must not send extra fields other than those documented in the DDP protocol, in
case these extra fields have meaning to future servers.  On the server, all
field changes must be optional/ignorable for compatibility with older clients;
otherwise a new protocol version would be required.

## Establishing a DDP Connection:

### Messages:

* `connect` (client -> server)
    - `session`: string (if trying to reconnect to an existing DDP session)
    - `version`: string (the proposed protocol version)
    - `support`: array of strings (protocol versions supported by the client,
      in order of preference)
* `connected` (server->client)
    - `session`: string (an identifier for the DDP session)
* `failed` (server->client)
    - `version`: string (a suggested protocol version to connect with)

### Procedure:

* The client sends a `connect` message.
* If the server is willing to speak the `version` of the protocol specified in
  the `connect` message, it sends back a `connected` message.
* Otherwise the server sends back a `failed` message with a version of DDP it
  would rather speak, informed by the `connect` message's `support` field, and
  closes the underlying transport.

## Heartbeats

### Messages:

* `ping`
    - `id`: optional string (identifier used to correlate with response)
* `pong`
    - `id`: optional string (same as received in the `ping` message)

### Procedure:

At any time after the connection is established either side may send a `ping`
message. The sender may chose to include an `id` field in the `ping`
message. When the other side receives a `ping` it must immediately respond with
a `pong` message. If the received `ping` message includes an `id` field, the
`pong` message must include the same `id` field.

## Managing Data:

### Messages:

* `sub` (client -> server):
    - `id`: string (an arbitrary client-determined identifier for this subscription)
    - `name`: string (the name of the subscription)
    - `params`: optional array of EJSON items (parameters to the subscription)
* `unsub` (client -> server):
    - `id`: string (the id passed to 'sub')
* `nosub` (server -> client):
    - `id`: string (the id passed to 'sub')
    - `error`: optional Error (an error raised by the subscription as it
      concludes, or sub-not-found)
* `added` (server -> client):
    - `collection`: string (collection name)
    - `id`: string (document ID)
    - `fields`: optional object with EJSON values
* `changed` (server -> client):
    - `collection`: string (collection name)
    - `id`: string (document ID)
    - `fields`: optional object with EJSON values
    - `cleared`: optional array of strings (field names to delete)
* `removed` (server -> client):
    - `collection`: string (collection name)
    - `id`: string (document ID)
* `ready` (server -> client):
    - `subs`: array of strings (ids passed to 'sub' which have sent their
      initial batch of data)
* `addedBefore` (server -> client):
    - `collection`: string (collection name)
    - `id`: string (document ID)
    - `fields`: optional object with EJSON values
    - `before`: string or null (the document ID to add the document before,
      or null to add at the end)
* `movedBefore` (server -> client):
    - `collection`: string
    - `id`: string (the document ID)
    - `before`: string or null (the document ID to move the document before, or
      null to move to the end)

### Procedure:

* The client specifies sets of information it is interested in by sending
  `sub` messages to the server.

* At any time, but generally informed by the `sub` messages, the server can
  send data messages to the client.  Data consist of `added`, `changed`, and
  `removed` messages.  These messages model a local set of data the client
  should keep track of.

    - An `added` message indicates a document was added to the local set. The ID
      of the document is specified in the `id` field, and the fields of the
      document are specified in the `fields` field.  Minimongo interperets the
      string id field in a special way that transforms it to the _id field of
      Mongo documents.

    - A `changed` message indicates a document in the local set has new values
      for some fields or has had some fields removed. The `id` field is the ID of
      the document that has changed.  The `fields` object, if present, indicates
      fields in the document that should be replaced with new values.  The
      `cleared` field contains an array of fields that are no longer in the
      document.

    - A `removed` message indicates a document was removed from the local
      set. The `id` field is the ID of the document.

* A collection is either ordered, or not. If a collection is ordered,
  the `added` message is replaced by `addedBefore`, which
  additionally contains the ID of the document after the one being
  added in the `before` field.  If the document is being added at the
  end, `before` is set to null. For a given collection, the server
  should only send `added` messages or `addedBefore` messages, not a
  mixture of both, and should only send `movedBefore` messages for a
  collection with `addedBefore` messages.

* The client maintains one set of data per collection.  Each subscription does
  not get its own datastore, but rather overlapping subscriptions cause the
  server to send the union of facts about the one collection's data.  For
  example, if subscription A says document `x` has fields `{foo: 1, bar: 2}`
  and subscription B says document `x` has fields `{foo: 1, baz:3}`, then the
  client will be informed that document `x` has fields `{foo: 1, bar: 2, baz:
   3}`.  If field values from different subscriptions conflict with each other,
  the server should send one of the possible field values.

* When one or more subscriptions have finished sending their initial batch of
  data, the server will send a `ready` message with their IDs.

## Remote Procedure Calls:

### Messages:

* `method` (client -> server):
    - `method`: string (method name)
    - `params`: optional array of EJSON items (parameters to the method)
    - `id`: string (an arbitrary client-determined identifier for this method call)
    - `randomSeed`: optional JSON value (an arbitrary client-determined seed
      for pseudo-random generators)
* `result` (server -> client):
    - `id`: string (the id passed to 'method')
    - `error`: optional Error (an error thrown by the method (or method-not-found)
    - `result`: optional EJSON item (the return value of the method, if any)
* `updated` (server -> client):
    - `methods`: array of strings (ids passed to 'method', all of whose writes
      have been reflected in data messages)

### Procedure:

* The client sends a `method` message to the server

* The server responds with a `result` message to the client, carrying either
  the result of the method call, or an appropriate error.

* Method calls can affect data that the client is subscribed to.  Once the
  server has finished sending the client all the relevant data messages based
  on this procedure call, the server should send an `updated` message to the
  client with this method's ID.

* There is no particular required ordering between `result` and `updated`
  messages for a method call.

* The client may provide a randomSeed JSON value.  If provided, this value is
  used to seed pseudo-random number generation.  By using the same seed with
  the same algorithm, the same pseudo-random values can be generated on the
  client and the server.  In particular, this is used for generating ids for
  newly created documents.  If randomSeed is not provided, then values
  generated on the server and the client will not be identical.

* Currently randomSeed is expected to be a string, and the algorithm by which
  values are produced from this is not yet documented.  It will likely be
  formally specified in future when we are confident that the complete
  requirements are known, or when a compatible implementation requires this
  to be specified.

## Errors:

Errors appear in `result` and `nosub` messages in an optional error field. An
error is an Object with the following fields:

* `error`: string (previously a number. See appendix 3)
* `reason`: optional string
* `message`: optional string
* `errorType`: pre-defined string with a value of `Meteor.Error`

Such an Error is used to represent errors raised by the method or subscription,
as well as an attempt to subscribe to an unknown subscription or call an unknown
method.

Other erroneous messages sent from the client to the server can result in
receiving a top-level `msg: 'error'` message in response. These conditions
include:

* sending messages which are not valid JSON objects
* unknown `msg` type
* other malformed client requests (not including required fields)
* sending anything other than `connect` as the first message, or sending
  `connect` as a non-initial message

The error message contains the following fields:

* `reason`: string describing the error
* `offendingMessage`: if the original message parsed properly, it is included
  here
