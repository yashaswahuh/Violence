package protocol

import (
	"errors"
	"fmt"
	"regexp"
	"strings"

	"github.com/yashaswahuh/Violence/backend/config"
)

// Server -> Client frame types
const (
	FrameWelcome = "welcome"
	FrameState   = "state"
	FrameQueue   = "queue"
	FrameMembers = "members"
	FramePong    = "pong"
	FrameError   = "error"
	FrameBye     = "bye"
	FrameActivity = "activity"
)

// Client -> Server frame types
const (
	FramePing      = "ping"
	FrameControl   = "control"
	FrameSync      = "sync"
	FrameSyncQueue = "syncQueue"
	FrameReport    = "report"
)

// Control actions
const (
	ActionPlay        = "play"
	ActionPause       = "pause"
	ActionSeek        = "seek"
	ActionSetTrack    = "setTrack"
	ActionSetQueue    = "setQueue"
	ActionQueueAdd    = "queueAdd"
	ActionQueueRemove = "queueRemove"
	ActionQueueClear  = "queueClear"
	ActionQueueMove   = "queueMove"
	ActionNext        = "next"
	ActionPrevious    = "previous"
	ActionKick        = "kick"
	ActionSetMaxMembers = "setMaxMembers"
	ActionSetAutoplay = "setAutoplay"
	ActionSetHostOnlyControl = "setHostOnlyControl"
)

// ControlActions are the actions a party's HostOnlyControl setting restricts to
// the host. Membership actions are not here: kick and setMaxMembers are already
// host-only unconditionally, and setHostOnlyControl has to stay reachable by
// the host to be turned back off.
var ControlActions = map[string]bool{
	ActionPlay:        true,
	ActionPause:       true,
	ActionSeek:        true,
	ActionSetTrack:    true,
	ActionSetQueue:    true,
	ActionQueueAdd:    true,
	ActionQueueRemove: true,
	ActionQueueClear:  true,
	ActionQueueMove:   true,
	ActionNext:        true,
	ActionPrevious:    true,
	ActionSetAutoplay: true,
}

// JoinRequest is the identity submitted when creating or joining a party.
type JoinRequest struct {
	UserId      string  `json:"userId"`
	DeviceId    string  `json:"deviceId"`
	DisplayName string  `json:"displayName"`
	AvatarUrl   *string `json:"avatarUrl,omitempty"`
	MaxMembers  *int    `json:"maxMembers,omitempty"`
	AutoplayEnabled *bool `json:"autoplayEnabled,omitempty"`

	// In-app profile identity (Phase 8). All optional and purely cosmetic: a client without a
	// profile, or an older client, sends none of them. Validate drops (never rejects) a value
	// that is malformed, so a bad extra can never stop anybody from joining.
	ProfileId   *string `json:"profileId,omitempty"`
	AvatarId    *string `json:"avatarId,omitempty"`
	AvatarColor *string `json:"avatarColor,omitempty"`
}

var (
	profileIdPattern    = regexp.MustCompile(`^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$`)
	avatarIdPattern     = regexp.MustCompile(`^[a-z0-9]{1,16}$`)
	avatarColorPattern  = regexp.MustCompile(`^[0-9a-f]{6}$`)
)

// cleanOptional returns the trimmed, lower-cased value if it matches pattern, otherwise nil.
func cleanOptional(value *string, pattern *regexp.Regexp) *string {
	if value == nil {
		return nil
	}
	v := strings.ToLower(strings.TrimSpace(*value))
	if !pattern.MatchString(v) {
		return nil
	}
	return &v
}

// Validate ensures all required identity fields are present and safe.
func (r *JoinRequest) Validate() error {
	r.UserId = strings.TrimSpace(r.UserId)
	if r.UserId == "" || len(r.UserId) > 128 {
		return errors.New("userId must be between 1 and 128 characters")
	}

	r.DeviceId = strings.TrimSpace(r.DeviceId)
	if r.DeviceId == "" || len(r.DeviceId) > 128 {
		return errors.New("deviceId must be between 1 and 128 characters")
	}

	r.DisplayName = strings.TrimSpace(r.DisplayName)
	if r.DisplayName == "" || len(r.DisplayName) > 80 {
		return errors.New("displayName must be between 1 and 80 characters")
	}

	if r.AvatarUrl != nil {
		trimmed := strings.TrimSpace(*r.AvatarUrl)
		if trimmed == "" {
			r.AvatarUrl = nil
		} else if len(trimmed) > 1000 {
			return errors.New("avatarUrl must not exceed 1000 characters")
		} else if !strings.HasPrefix(trimmed, "http://") && !strings.HasPrefix(trimmed, "https://") {
			return errors.New("avatarUrl must be an http(s) URL")
		} else {
			r.AvatarUrl = &trimmed
		}
	}
	if r.MaxMembers != nil && (*r.MaxMembers < 2 || *r.MaxMembers > config.MaxPartyMembers) {
		return fmt.Errorf("maxMembers must be between 2 and %d", config.MaxPartyMembers)
	}

	// The avatar id is deliberately not checked against a list of presets here: adding an avatar
	// to the apps must not need a server redeploy.
	r.ProfileId = cleanOptional(r.ProfileId, profileIdPattern)
	r.AvatarId = cleanOptional(r.AvatarId, avatarIdPattern)
	r.AvatarColor = cleanOptional(r.AvatarColor, avatarColorPattern)
	return nil
}
