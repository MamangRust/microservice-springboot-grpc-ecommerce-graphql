-- Create order_outbox table
-- Transactional outbox for order events (currently stats.order.event). Mirrors
-- the sibling outbox tables (auth / transaction / merchant) so the shared
-- KafkaOutboxPublisher relay can drain it with identical lease/backoff semantics.
CREATE TABLE IF NOT EXISTS "order_outbox" (
    "id" BIGSERIAL PRIMARY KEY,
    "event_id" VARCHAR(100) NOT NULL UNIQUE,
    "topic" VARCHAR(200) NOT NULL,
    "event_key" VARCHAR(200),
    "payload" TEXT NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "attempts" INT NOT NULL DEFAULT 0,
    "next_attempt_at" TIMESTAMP NULL,
    "claimed_at" TIMESTAMP NULL,
    "claim_token" VARCHAR(100) NULL,
    "last_error" TEXT NULL,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS "idx_order_outbox_due"
    ON "order_outbox" ("status", "next_attempt_at", "claimed_at");

CREATE INDEX IF NOT EXISTS "idx_order_outbox_event_id"
    ON "order_outbox" ("event_id");
