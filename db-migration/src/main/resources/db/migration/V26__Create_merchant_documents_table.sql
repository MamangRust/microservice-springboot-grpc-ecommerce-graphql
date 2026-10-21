-- Create merchant_documents table
-- Backs the merchant-document flows. The table was missing entirely in the
-- source project (the entity/protos existed but no migration shipped), so this
-- is new schema rather than a port.
CREATE TABLE IF NOT EXISTS "merchant_documents" (
    "document_id" SERIAL PRIMARY KEY,
    "merchant_id" INT NOT NULL REFERENCES "merchants" ("id") ON DELETE CASCADE,
    "document_type" VARCHAR(100) NOT NULL,
    "document_url" TEXT NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "note" TEXT,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deleted_at" TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS "idx_merchant_documents_merchant_id" ON "merchant_documents" ("merchant_id");
CREATE INDEX IF NOT EXISTS "idx_merchant_documents_status" ON "merchant_documents" ("status");
CREATE INDEX IF NOT EXISTS "idx_merchant_documents_deleted_at" ON "merchant_documents" ("deleted_at");
