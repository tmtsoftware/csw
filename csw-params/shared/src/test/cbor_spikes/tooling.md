## Tools/Utilities helpful while debugging cbor encoding/decoding


#### For json representation of cbor

`cbor2json.rb csw-params/shared/src/test/cbor_spikes/data/event.cbor`

#### For validating cbor against the schema

`cddl csw-params/shared/src/test/cbor_spikes/schema/event_command.cddl validate csw-params/shared/src/test/cbor_spikes/data/event.cbor`

#### For generating sample jsons for your schema

This command helps while writing schema. It guides you with what all data can fit into the schema.

`cddl csw-params/shared/src/test/cbor_spikes/event_command.cddl json-generate`

#### More tools 
Explore: https://github.com/cabo/cbor-diag