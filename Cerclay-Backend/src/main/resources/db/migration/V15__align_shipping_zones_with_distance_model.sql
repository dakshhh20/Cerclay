-- Shipping is distance/zone based only.
-- No weight, volumetric weight, or weight slab is used by the customer shipping engine.

UPDATE shipping_zones
SET code = 'C',
    name = 'Metro-to-Metro'
WHERE code = 'C_D';

INSERT INTO shipping_zones
    (code, name, shadowfax_base_rate, customer_charge, free_delivery_threshold, cod_charge, active)
SELECT 'D', 'Rest of India', 59.00, 59.00, 0.00, 0.00, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM shipping_zones WHERE code = 'D'
);

UPDATE shipping_zones
SET name = CASE code
    WHEN 'A' THEN 'Local'
    WHEN 'B' THEN 'Regional'
    WHEN 'C' THEN 'Metro-to-Metro'
    WHEN 'D' THEN 'Rest of India'
    WHEN 'E' THEN 'Special'
    ELSE name
END
WHERE code IN ('A', 'B', 'C', 'D', 'E');
