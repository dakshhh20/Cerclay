# Mitti & More Shipping Zone Classification

The customer-facing shipping engine is distance/zone based only. Product weight and volumetric weight are not used.

## Resolution order

1. Shadowfax serviceability is checked first.
2. An explicit admin-created `shipping_pincodes` mapping wins when present and active.
3. Otherwise `PincodeZoneClassifier` automatically determines Zone A/B/C/D/E from the destination pincode, configured pickup pincode/state, and compact India-wide postal prefix rules.
4. The resolved zone loads its admin-configured customer charge, free-delivery threshold, and COD charge.

## Zones

- A: Local — same three-digit postal prefix as the configured pickup pincode.
- B: Regional — same state, different city, when not Metro-to-Metro.
- C: Metro-to-Metro — both pickup and destination match the configured metro PIN-prefix set.
- D: Rest of India — different state / other non-special destinations.
- E: Special — Northeast, J&K/Ladakh, Andaman & Nicobar, and other configured special PIN ranges.

## Data source and maintenance

The postal prefix rules are a compact classification layer, not a frozen 20k-row postal-directory copy. India Post's public directory is updated periodically. Exact pincode overrides can always be created through the admin shipping API and take precedence over automatic classification.

For production, the metro/special prefix lists should be reviewed whenever the business changes warehouse location or its definition of metro/special delivery areas.
