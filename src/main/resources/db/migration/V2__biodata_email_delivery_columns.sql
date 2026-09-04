-- Post-payment "email me the PDF" delivery (commit b470563) added these fields to
-- the BioData entity but never shipped a migration for them, leaving prod's
-- bio_data table out of sync with Hibernate's expected schema.

ALTER TABLE bio_data ADD COLUMN IF NOT EXISTS deliver_by_email BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE bio_data ADD COLUMN IF NOT EXISTS pdf_emailed_at   TIMESTAMP;
