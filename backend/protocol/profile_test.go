package protocol

import (
	"strings"
	"testing"
)

func optStr(s string) *string { return &s }

func TestProfileFieldsAreOptional(t *testing.T) {
	req := validRequest()
	if err := req.Validate(); err != nil {
		t.Fatalf("Validate() = %v, want nil", err)
	}
	if req.ProfileId != nil || req.AvatarId != nil || req.AvatarColor != nil {
		t.Errorf("absent profile fields must stay nil, got %v %v %v", req.ProfileId, req.AvatarId, req.AvatarColor)
	}
}

func TestProfileFieldsAreKeptAndNormalised(t *testing.T) {
	req := validRequest()
	req.ProfileId = optStr("  123E4567-E89B-42D3-A456-426614174000 ")
	req.AvatarId = optStr(" Guitar ")
	req.AvatarColor = optStr("0A84FF")
	if err := req.Validate(); err != nil {
		t.Fatalf("Validate() = %v, want nil", err)
	}
	if got := *req.ProfileId; got != "123e4567-e89b-42d3-a456-426614174000" {
		t.Errorf("profileId = %q", got)
	}
	if got := *req.AvatarId; got != "guitar" {
		t.Errorf("avatarId = %q", got)
	}
	if got := *req.AvatarColor; got != "0a84ff" {
		t.Errorf("avatarColor = %q", got)
	}
}

// A malformed optional field is dropped, never an error: nobody may be unable to join because
// of a cosmetic extra.
func TestInvalidProfileFieldsAreDroppedNotRejected(t *testing.T) {
	cases := []struct {
		name   string
		mutate func(*JoinRequest)
		check  func(*JoinRequest) *string
	}{
		{"short profileId", func(r *JoinRequest) { r.ProfileId = optStr("123") }, func(r *JoinRequest) *string { return r.ProfileId }},
		{"non-hex profileId", func(r *JoinRequest) { r.ProfileId = optStr("zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz") }, func(r *JoinRequest) *string { return r.ProfileId }},
		{"profileId with trailing text", func(r *JoinRequest) { r.ProfileId = optStr("123e4567-e89b-42d3-a456-426614174000<b>") }, func(r *JoinRequest) *string { return r.ProfileId }},
		{"empty avatarId", func(r *JoinRequest) { r.AvatarId = optStr("") }, func(r *JoinRequest) *string { return r.AvatarId }},
		{"long avatarId", func(r *JoinRequest) { r.AvatarId = optStr(strings.Repeat("a", 17)) }, func(r *JoinRequest) *string { return r.AvatarId }},
		{"avatarId with markup", func(r *JoinRequest) { r.AvatarId = optStr("<script>") }, func(r *JoinRequest) *string { return r.AvatarId }},
		{"hash colour", func(r *JoinRequest) { r.AvatarColor = optStr("#0a84ff") }, func(r *JoinRequest) *string { return r.AvatarColor }},
		{"short colour", func(r *JoinRequest) { r.AvatarColor = optStr("fff") }, func(r *JoinRequest) *string { return r.AvatarColor }},
		{"non-hex colour", func(r *JoinRequest) { r.AvatarColor = optStr("gggggg") }, func(r *JoinRequest) *string { return r.AvatarColor }},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			req := validRequest()
			c.mutate(&req)
			if err := req.Validate(); err != nil {
				t.Fatalf("Validate() = %v, want nil (an invalid optional field is dropped, not rejected)", err)
			}
			if got := c.check(&req); got != nil {
				t.Errorf("invalid field kept as %q", *got)
			}
		})
	}
}

func TestProfileFieldsDoNotExcuseMissingRequiredFields(t *testing.T) {
	req := validRequest()
	req.ProfileId = optStr("123e4567-e89b-42d3-a456-426614174000")
	req.DisplayName = " "
	if err := req.Validate(); err == nil {
		t.Errorf("a profile must not excuse an empty displayName")
	}
}
