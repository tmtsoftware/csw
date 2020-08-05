# Change Log

## [v2] - 2020-03-20

- Encoding of `ComponentType` model has changed from snake case to retain original case
    old: { "componentType": "sequence_component" }
    new: { "componentType": "SequenceComponent" }

- Encoding/Decoding for `Parameter` values of type `Char` has changed from ascii to retain original char
    old: "CharKey": { "values": [ 65, 66 ] }
    new: "CharKey": { "values": [ "A", "B" ] }

- Encoding/Decoding for `AlarmSeverity` has changed from lower case to retain original case
    old: "AlarmSeverity": "critical"
    new: "AlarmSeverity": "Critical"

## [v1] 

- Initial version
