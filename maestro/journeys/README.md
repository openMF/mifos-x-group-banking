# Maestro E2E journeys (MifosSave)

Device-runnable end-to-end flows covering the app's main journeys, verified on a physical device
against the live companion (`mifossave-companion.onrender.com`) + Fineract `mifos-bank-2`.

```bash
export PATH="$PATH:$HOME/.maestro/bin"
maestro test maestro/journeys/                       # whole suite
maestro test maestro/journeys/04-member-profile.yaml # one flow
maestro --device <serial> test maestro/journeys/...  # target a device
```

## Demo personas (passwords differ per persona)

| Persona | Phone | Password | Role → landing |
|---------|-------|----------|----------------|
| Amina Otieno | +254700000001 | `DemoExplore@2026` | Treasurer → organizer-dashboard |
| Joseph Mwangi | +254700000002 | `DemoChair@2026` | Chairperson → organizer-dashboard |
| Grace Wanjiru | +254700000003 | `DemoMember@2026` | Member → personal-dashboard |
| David Ochieng | +254700000010 | `DemoField@2026` | Field officer |

## Flows

| Flow | Verifies |
|------|----------|
| `01-login-member` | member (Grace) login → personal dashboard with real groups |
| `02-login-organizer` | leader (Amina) login → **organizer** dashboard, renders content not shimmer |
| `03-group-list` | "My Groups" loads real cards (guards the fineractGroupId / date deserialization fixes) |
| `04-member-profile` | group → Members → member profile renders (savings history, role, attendance) |
| `05-loan-detail` | group → Loans → loan detail renders (amortization schedule) |
| `06-savings` | group → My Savings resolves to the transaction list (not the shimmer) |
| `07-meetings` | organizer Today's-Schedule row → meeting calendar (upcoming + past meetings) |
| `08-full-read-journey` | the whole read path in one flow: list → detail → members → profile → loans → detail |
| `09-write-group-create` | WRITE: create a VSLA group through the 4-step wizard → `POST /companion/groups` |
| `10-write-loan-apply-form` | WRITE (form-load): the Apply-for-Loan form reaches the user ready to submit |

### Write journeys — how they're verified

The write **mutations** are verified reliably at the API level in `server-layer/verify` (a real group
is created with the creator auto-associated; role-update / meeting-record / loan routes accept + wire
correctly). The Maestro write flows here drive the on-device **screens**: `10` confirms the form loads
its template data and reaches a submittable state; `09` drives the full multi-step create wizard.

> **Multi-step Compose wizard caveat**: Maestro automation of a scrolling wizard with dropdowns +
> keyboard is timing-sensitive — a dropdown selection that doesn't register leaves step-1 validation
> (officeId/meetingDay/meetingTime) blocking Next. `09` is best-effort; if it stalls on Identity,
> re-run or raise the `waitForAnimationToEnd` waits. The mutation itself is not in doubt (API-verified).

## Notes / gotchas (learned the hard way)

- **Backend flaps**: mifos-bank-2 intermittently 502s; the group-list then shows "Could not load
  groups". Flows tolerate it with a `retry` + a `runFlow when: visible "Could not load groups"` that
  taps **Retry**. A persistent failure is a real regression, not a flap.
- **Wait on CONTENT, not the screen**: a bare `assertVisible: <screen_id>` passes while the shimmer is
  still up. Always `extendedWaitUntil` a content tag (`*_lazy_column`, `*_content_list`, `*_transaction_list`)
  and `assertNotVisible` the loading indicator before screenshotting.
- **Meetings** is reached from the **organizer dashboard's Today's Schedule** row (NavigateToMeetingCalendar),
  NOT a group-dashboard quick action.
- **Create Group** (group-list FAB) opens a **group-type picker** first, then the create form.
- **`clearState: true`** fails on some OEMs (CLEAR_APP_USER_DATA perm) — omit it; `adb uninstall`+reinstall
  for a truly pristine state (ColorOS `pm clear` does not fully wipe EncryptedSharedPreferences).
