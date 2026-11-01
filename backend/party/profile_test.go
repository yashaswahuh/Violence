package party

import "testing"

func optStr(s string) *string { return &s }

func TestJoinWithProfileStoresAndSerialisesTheFields(t *testing.T) {
	store := NewPartyStore()
	p, err := store.Create()
	if err != nil {
		t.Fatalf("Failed to create party: %v", err)
	}
	profile := Profile{ProfileId: optStr("123e4567-e89b-42d3-a456-426614174000"), AvatarId: optStr("guitar"), AvatarColor: optStr("0a84ff")}
	m, err := p.JoinWithProfile("u1", "d1", "Alex", nil, profile)
	if err != nil {
		t.Fatalf("Join failed: %v", err)
	}
	wire := m.ToWire()
	if wire["profileId"] != "123e4567-e89b-42d3-a456-426614174000" || wire["avatarId"] != "guitar" || wire["avatarColor"] != "0a84ff" {
		t.Errorf("wire = %v", wire)
	}
}

func TestMembersWithoutAProfileHaveNoProfileKeys(t *testing.T) {
	store := NewPartyStore()
	p, _ := store.Create()
	m, err := p.Join("u1", "d1", "Alex", nil)
	if err != nil {
		t.Fatalf("Join failed: %v", err)
	}
	wire := m.ToWire()
	for _, key := range []string{"profileId", "avatarId", "avatarColor"} {
		if _, present := wire[key]; present {
			t.Errorf("%s must be absent for a member without a profile", key)
		}
	}
}

func TestRejoinTakesTheNewProfileIncludingItsAbsence(t *testing.T) {
	store := NewPartyStore()
	p, _ := store.Create()
	first, _ := p.JoinWithProfile("u1", "d1", "Alex", nil, Profile{ProfileId: optStr("123e4567-e89b-42d3-a456-426614174000"), AvatarId: optStr("cat"), AvatarColor: optStr("ff9500")})
	again, err := p.JoinWithProfile("u1", "d1", "Alex", nil, Profile{})
	if err != nil {
		t.Fatalf("Re-join failed: %v", err)
	}
	if again.MemberId != first.MemberId {
		t.Fatalf("re-join must keep the member")
	}
	if again.ProfileId != nil || again.AvatarId != nil || again.AvatarColor != nil {
		t.Errorf("a profile deleted on the device must be cleared on re-join")
	}
}
