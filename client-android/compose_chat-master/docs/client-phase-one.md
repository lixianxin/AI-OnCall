# Client Phase One

## Goal

Phase one adds an independent open-tab client layer without removing the original compose_chat IM demo logic.

## Scope

- Open API config with `containerVersion = 1` and default mock server base URL `http://10.0.2.2:8080`.
- Session holder for the training-camp server token, user info, and business permissions.
- REST client skeleton based on OkHttp.
- JSON DTOs for login, user info, debug status, approval summary, and `TabManifest`.
- Local mock `TabManifest` list for early client development.
- Dynamic bottom navigation entries rendered beside the original demo tabs.
- Native/web/external tab placeholders and clear restricted states.

## Supported Mock Tabs

| id | route | entryType | purpose |
|---|---|---|---|
| approval | /approval | native | Native business tab placeholder |
| calendar | /calendar | native | Native calendar placeholder |
| finance | /finance | native | Permission-denied demo |
| docs | /docs | web | Web tab placeholder |
| ai-oncall | /ai-oncall | native | AI OnCall page shell |
| legacy-hybrid | /legacy-hybrid | hybrid | Unsupported entry type demo |
| future-tab | /future | native | Container version mismatch demo |

## First Integration Contract

The client models follow the server interface draft:

- `POST /auth/login`
- `GET /me`
- `GET /tabs`
- `GET /debug/status`
- `GET /business/approval/summary`
- `GET /oncall/stream` is reserved for phase two SSE work.

The current UI uses local mock tabs first, so Android development is not blocked by server readiness.

## Notes

- The original Tencent IM login and chat provider are preserved.
- Training-camp server state is stored separately in `OpenSessionManager`.
- `TabManifest` is converted into `OpenTabItem` before rendering so permission, version, route, and entry-type checks stay in one place.
