# Impact analysis

Impact analysis walks incoming graph edges from a changed symbol. It reports
affected nodes and dependency paths, then classifies risk using transparent
heuristics: API exposure, database nodes, and large blast radius increase risk.
This is a review aid and cannot model reflection or runtime feature flags.
