package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/yashaswahuh/Violence/backend/config"
)

// The profile fields travel through create, join and the member list, the preview that anyone
// holding the code can read shows only the cosmetic ones, and a bad field never blocks a join.
func TestProfileIdentityOverHTTP(t *testing.T) {
	createLimiter = newIPRateLimiter(time.Minute, config.CreateRatePerMinute, config.RateLimitMaxEntries)
	mux := http.NewServeMux()
	mux.HandleFunc("POST /api/parties", handleCreateParty)
	mux.HandleFunc("POST /api/parties/{code}/join", handleJoinParty)
	mux.HandleFunc("GET /api/parties/{code}/preview", handlePreviewParty)
	ts := httptest.NewServer(corsMiddleware(mux))
	defer ts.Close()

	post := func(path string, body map[string]interface{}) (int, map[string]interface{}) {
		b, _ := json.Marshal(body)
		res, err := http.Post(ts.URL+path, "application/json", bytes.NewReader(b))
		if err != nil {
			t.Fatalf("POST %s: %v", path, err)
		}
		defer res.Body.Close()
		var out map[string]interface{}
		_ = json.NewDecoder(res.Body).Decode(&out)
		return res.StatusCode, out
	}

	const pid = "123e4567-e89b-42d3-a456-426614174000"
	status, created := post("/api/parties", map[string]interface{}{
		"userId": "u_alex", "deviceId": "d_alex", "displayName": "Alex",
		"profileId": pid, "avatarId": "guitar", "avatarColor": "0A84FF",
	})
	if status != http.StatusCreated {
		t.Fatalf("create status = %d", status)
	}
	you := created["you"].(map[string]interface{})
	if you["profileId"] != pid || you["avatarId"] != "guitar" || you["avatarColor"] != "0a84ff" {
		t.Errorf("creator wire = %v", you)
	}
	code := created["code"].(string)

	// A joiner with garbage in the optional fields still joins, and carries none of them.
	status, joined := post("/api/parties/"+code+"/join", map[string]interface{}{
		"userId": "u_bob", "deviceId": "d_bob", "displayName": "Bob",
		"profileId": "not-a-uuid", "avatarId": "<b>", "avatarColor": "#fff",
	})
	if status != http.StatusOK {
		t.Fatalf("join with bad optional fields must still succeed, got %d", status)
	}
	bob := joined["you"].(map[string]interface{})
	for _, key := range []string{"profileId", "avatarId", "avatarColor"} {
		if _, present := bob[key]; present {
			t.Errorf("%s must be dropped, got %v", key, bob[key])
		}
	}

	// An old client (no optional fields at all) joins exactly as before.
	status, _ = post("/api/parties/"+code+"/join", map[string]interface{}{
		"userId": "u_old", "deviceId": "d_old", "displayName": "Old client",
	})
	if status != http.StatusOK {
		t.Fatalf("old-style join status = %d", status)
	}

	res, err := http.Get(ts.URL + "/api/parties/" + code + "/preview")
	if err != nil {
		t.Fatalf("preview: %v", err)
	}
	defer res.Body.Close()
	if res.StatusCode != http.StatusOK {
		t.Fatalf("preview status = %d", res.StatusCode)
	}
	var preview map[string]interface{}
	_ = json.NewDecoder(res.Body).Decode(&preview)
	members := preview["members"].([]interface{})
	first := members[0].(map[string]interface{})
	if first["avatarId"] != "guitar" || first["avatarColor"] != "0a84ff" {
		t.Errorf("preview should show avatar and colour, got %v", first)
	}
	if _, leaked := first["profileId"]; leaked {
		t.Errorf("the preview must never carry the profile id")
	}
}
