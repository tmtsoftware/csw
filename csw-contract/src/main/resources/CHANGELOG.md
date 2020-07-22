# Change Log

## [v2] - 2020-03-20

- Encoding of `ComponentType` model has changed from snake case to retain original case
    old: { "componentType": "sequence_component" }
    new: { "componentType": "SequenceComponent" }

- Encoding/Decoding for `Parameter` values of type `Char` has changed from ascii to retain original char
    old: "CharKey": { "values": [ 65, 66 ] }
    new: "CharKey": { "values": [ "A", "B" ] }

## [v1] 

- Initial version
