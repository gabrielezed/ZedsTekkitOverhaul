# Future territorial activity and threat

Design contract for a later milestone.

| Responsibility | Contract |
| --- | --- |
| TerritorialActivityService | Summarize visits and sustained presence on the owning simulation thread. Decay old observations and saturate repeated activity. It has no concept of a base or settlement. |
| ThreatAssessment | Combine bounded activity signals with independent regional danger, world context and meaningful progression. Return a capped local assessment with a reason. Activity can coexist with low threat, and an unvisited region can be dangerous. |
| EncounterDirector | Choose the encounter and its intensity from the scheduling window, target and optional assessment. PLAYER_ASSAULT queries around the player. A future SITE_SIEGE would take an independently established site target. |

Candidate inputs include time spent in an area, spaced visits, biome or authored regional danger and discrete progression milestones. Presence should be sampled sparingly and movement across a boundary should not count as extra activity on its own. Factory size, wealth and world age should not directly increase danger. No base marker should be required. Advanced gear should continue to make previously mastered encounters manageable.

Single chunks give precise boundaries but amplify boundary hopping and produce many entries. Larger fixed regions reduce memory and I/O while blurring nearby outposts and producing visible edges. Compare coarse cells with capped neighbor blending and measure how the choice affects walking, portals and distant activity. Keep dimensions separate. A future save format needs a version, a bounded number of records, predictable eviction, lazy decay and batched writes. It must never visit or load chunks to maintain scores.

The smallest future integration is one assessment when a natural PLAYER_ASSAULT is admitted. Let the director select a bounded encounter budget from it and retain the current configuration as the fallback. The lunar service remains the scheduling authority. The placement rules and both population caps remain enforceable regardless of assessment.

Before implementation, compare sessions involving brief visits, stationary work, travel, long absences and late-game equipment. Verify decay, saturation, region edges, persistence cost, old-save behavior and resistance to trivial activity farming. Validate that a player can understand and manage local pressure without prescribed base layouts, that new factories are viable, and that a dangerous region can matter without an activity history.
