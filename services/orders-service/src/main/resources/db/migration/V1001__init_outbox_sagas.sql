-- Outbox for orders-service
CREATE TABLE IF NOT EXISTS public.outbox (
  id UUID PRIMARY KEY,
  aggregate_type TEXT NOT NULL,
  aggregate_id TEXT NOT NULL,
  event_type TEXT NOT NULL,
  payload TEXT NOT NULL,
  headers TEXT NULL,
  status TEXT NOT NULL DEFAULT 'PENDING',
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
  published_at TIMESTAMP WITH TIME ZONE NULL,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_type ON public.outbox(aggregate_type);
CREATE INDEX IF NOT EXISTS idx_outbox_occurred_at ON public.outbox(occurred_at);

-- Sagas ledger (SagaStateEntity shape)
CREATE TABLE IF NOT EXISTS public.sagas (
  saga_id UUID PRIMARY KEY,
  saga_type TEXT NOT NULL,
  correlation_id TEXT NOT NULL,
  state TEXT NOT NULL,
  data TEXT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_sagas_correlation ON public.sagas(correlation_id);
CREATE INDEX IF NOT EXISTS idx_sagas_type ON public.sagas(saga_type);
