package services

import (
	"testing"
	"time"

	"digital_wallet_api/internal/dto"
	"github.com/google/uuid"
)

func TestNormalizeCursorDefaultsInvalidLimit(t *testing.T) {
	filter := dto.TransactionFilter{Limit: 1000}

	if err := normalizeCursor(&filter); err != nil {
		t.Fatalf("expected nil error, got %v", err)
	}

	if filter.Limit != 20 {
		t.Fatalf("expected default limit 20, got %d", filter.Limit)
	}
}

func TestCursorRoundTrip(t *testing.T) {
	id := uuid.New()
	createdAt := time.Now().UTC().Truncate(time.Microsecond)

	token := encodeCursor(createdAt, id)
	cursor, err := decodeCursor(token)

	if err != nil {
		t.Fatalf("expected nil error, got %v", err)
	}
	if !cursor.CreatedAt.Equal(createdAt) {
		t.Fatalf("expected created_at %s, got %s", createdAt, cursor.CreatedAt)
	}
	if cursor.ID != id {
		t.Fatalf("expected id %s, got %s", id, cursor.ID)
	}
}

func TestPaginateTransactionsReturnsNextCursor(t *testing.T) {
	firstID := uuid.New()
	secondID := uuid.New()
	items := []dto.TransactionResponse{
		{ID: firstID, CreatedAt: time.Now().UTC()},
		{ID: secondID, CreatedAt: time.Now().UTC().Add(-time.Minute)},
	}

	resp := paginateTransactions(items, 1)

	if !resp.HasMore {
		t.Fatalf("expected has_more true")
	}
	if resp.NextCursor == "" {
		t.Fatalf("expected next cursor")
	}
	if len(resp.Items) != 1 {
		t.Fatalf("expected one returned item")
	}
}
