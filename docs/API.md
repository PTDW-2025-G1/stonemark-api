# API

> Live reference: Swagger UI at `/swagger-ui/index.html` when the application runs.
> Route policy and roles: [`ARCHITECTURE.md`](ARCHITECTURE.md).

Paged endpoints accept `page`, `size` and `sort`.

## Public (no authentication)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/public/submissions` | Submit a photo (multipart: `file`, optional `latitude`, `longitude`, `notes`); honours `Idempotency-Key`. |
| GET | `/api/v1/public/marks` | Paged published marks; filters `q`, `monumentId`, `located`. |
| GET | `/api/v1/public/marks/{id}` | A published mark with its published occurrences and media ids. |
| GET | `/api/v1/public/monuments` | Paged monuments; filter `name`. |
| GET | `/api/v1/public/monuments/{id}` | A monument. |
| POST | `/api/v1/public/monuments/search/polygon` | Monuments inside a GeoJSON polygon. |
| GET | `/api/v1/public/media/{id}` | Media metadata. |
| GET | `/api/v1/public/media/{id}/optimized` | The web image derivative. |
| GET | `/api/v1/public/users/{id}` | Public user profile. |
| GET | `/api/v1/public/users/exists/by-username` | Username availability. |
| POST | `/api/v1/public/contact-requests` | Submit a contact request. |
| GET | `/api/v1/public/vision/health` | Vision server health. |

Media writes require authentication: `POST /api/v1/public/media` and
`DELETE /api/v1/public/media/{id}`.

## Authentication

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/auth/authenticate` | Username or email plus password; returns an access and refresh token. |
| POST | `/api/v1/auth/refresh` | Exchange a refresh token for a new token pair. |
| POST | `/api/v1/auth/logout` | Revoke tokens (bumps the token version). |

## Account (authenticated)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/account/me` | Current principal. |
| GET | `/api/v1/account/profile` | Profile. |
| PUT | `/api/v1/account/profile` | Update profile. |
| POST | `/api/v1/account/password` | Set a password. |
| PUT | `/api/v1/account/password` | Change the password. |
| DELETE | `/api/v1/account` | Delete the account. |

## Bookmarks (authenticated)

Bookmarks belong to the authenticated caller; the user is taken from the security
context, never from the request. Bookmarks point at catalogue resources — a mark or a
mark occurrence (a finding at a place); photos are not targets of their own. The
target field matching `type` (`mark` or `occurrence`) is null when the target is no
longer available.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/bookmarks` | The authenticated user's bookmarks. |
| POST | `/api/v1/bookmarks` | Bookmark a target (`{"targetType":"MARK","targetId":123}` or `targetType: "MARK_OCCURRENCE"`). |
| DELETE | `/api/v1/bookmarks/{bookmarkId}` | Remove a bookmark. |

## Review and moderation (REVIEWER or higher)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/admin/review` | List recorded decisions (labels). |
| GET | `/api/v1/admin/review/{submissionId}` | Decision for a submission. |
| GET | `/api/v1/admin/review/{submissionId}/suggestions` | Suggestions for a submission. |
| POST | `/api/v1/admin/review/{submissionId}/accept` | Accept an existing mark. |
| POST | `/api/v1/admin/review/{submissionId}/accept-as-new` | Accept as a new mark. |
| POST | `/api/v1/admin/review/{submissionId}/reject` | Reject the submission. |
| GET | `/api/v1/admin/moderation/processing-by-confidence` | Processing records in a confidence band. |
| GET | `/api/v1/admin/moderation/discovery/marks` | Draft marks. |
| GET | `/api/v1/admin/processing/submission/{submissionId}/status` | Processing status. |
| GET | `/api/v1/admin/processing/submission/{submissionId}/suggestions` | Mark suggestions. |
| POST | `/api/v1/admin/processing/submission/{submissionId}/reprocess` | Re-run processing. |

## Catalogue and operations (MODERATOR or higher)

| Method | Path | Purpose |
|---|---|---|
| GET / PUT / DELETE | `/api/v1/admin/marks`, `/api/v1/admin/marks/{id}` | Read, edit (including publish/archive) or soft-delete a mark. |
| GET / PUT / DELETE | `/api/v1/admin/occurrences`, `/api/v1/admin/occurrences/{id}` | Read, edit (place, period, publish/archive) or soft-delete an occurrence. |
| GET / DELETE | `/api/v1/admin/evidences`, `/api/v1/admin/evidences/{id}` | Read or delete evidence. |
| POST / PUT / DELETE | `/api/v1/admin/monuments`, `/api/v1/admin/monuments/{id}` | Create, update or delete a monument. |
| GET | `/api/v1/admin/submissions`, `/api/v1/admin/submissions/{id}` | Submission queue and detail. |
| GET / PATCH / DELETE | `/api/v1/admin/contact-requests`, `/api/v1/admin/contact-requests/{id}` | List, change status or delete contact requests. |
| POST | `/api/v1/admin/email/test` | Send a test email. |

## Users (ADMIN)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/admin/users` | Paged users; filters `username`, `email`, `enabled`. |
| GET / PATCH / DELETE | `/api/v1/admin/users/{id}` | Read, update or delete a user. |
