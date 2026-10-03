ALTER TABLE orders ADD COLUMN delivered_at DATETIME NULL;
UPDATE orders SET delivered_at = updated_at WHERE order_status = 'DELIVERED' AND delivered_at IS NULL;

CREATE TABLE return_photos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    return_request_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NULL,
    content_type VARCHAR(100) NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_return_photo_request FOREIGN KEY (return_request_id) REFERENCES return_requests(id)
);
CREATE INDEX idx_return_photos_return ON return_photos(return_request_id);
