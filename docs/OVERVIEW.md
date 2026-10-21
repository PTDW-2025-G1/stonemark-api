# Overview

> What StoneMark is for, the domain it models, and the direction of the product.
> HTTP surface: [`API.md`](API.md). Code structure: [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Purpose

Masons' marks are symbols carved into stone by stonemasons to identify their work,
provenance or crew. They survive on monuments — churches, walls, bridges, buildings —
but are poorly catalogued.

StoneMark builds that catalogue:

- **Collect** — submit a photo, optionally with a location; no account required.
- **Curate** — a moderator decides what each photo is: an existing mark, a new mark,
  or nothing.
- **Publish** — the curated catalogue of marks and where they were found is public.
- **Automate** — matching via shape descriptors is available now; moderation
  decisions become training labels for stronger matching later.

Collection comes first: automation depends on data that does not exist yet, so it is
kept off the critical path.

## Domain

### Marks and findings

A **Mark** is the design — a pattern such as a cross, a compass or initials. A
**MarkOccurrence** is a physical finding of that design. One design can be found in
many places; each finding is its own occurrence.

| Concept | Meaning |
|---|---|
| `Mark` | the design: title, description, cover media, publication status |
| `MarkOccurrence` | a finding: mark, optional monument, own coordinates (optional), period, publication status |
| `MarkEvidence` | a photo supporting an occurrence: media, submission, embedding |
| `Monument` | the place: name, description, address, coordinates |
| `MarkEvidenceSubmission` | a public submission: photo, coordinates, notes, status |
| `MarkEvidenceReview` | a moderator decision, recorded as a label |

### Place

An occurrence carries its own coordinates; that is the normal field case. A monument
link is optional. An occurrence with neither is a valid **unlocated** entry — a mark
identified without a known place. `Monument.location` is a coarse point and an
occurrence's coordinates need not coincide with it.

Administrative areas are not modelled; the catalogue works with coordinates.

### Lifecycle

`PublicationStatus` describes publishable content:

- `DRAFT` — not public (the default).
- `PUBLISHED` — visible through the public API.
- `ARCHIVED` — retained, not public.

`Mark` and `MarkOccurrence` each carry a status. Publishing a mark for the first time
publishes its existing occurrences; occurrences added later have their own lifecycle.

Removal is soft (`deletedAt`) for users and content (`User`, `Monument`, `Mark`,
`MarkOccurrence`, `MarkEvidence`). Transactional records — submissions, reviews,
bookmarks — are removed outright.

### People

A user holds one ordered role: `USER < REVIEWER < MODERATOR < ADMIN`, with
capabilities inherited upward. Reviewers clear the moderation queue, moderators
manage the catalogue, admins manage users. Contributors need no account.

## Automation

Matching is done with a classic shape descriptor (no trained model): the Vision
service turns an image into a vector, and the API ranks existing marks by cosine
similarity. Work is split by what it takes to be wrong:

- **Occurrences are matching.** Deciding whether a finding matches an existing mark
  can be assisted by the descriptor: above a confidence threshold the submission is
  accepted and its occurrence linked automatically; below it, the case is queued for a
  human.
- **Marks are judgement.** Deciding that a set of findings is a new *design* is
  always a human decision. The system never creates a mark; it only proposes clusters
  of unmatched findings once enough of them accumulate, for a moderator to action.

There is no detection model: the descriptor runs on whatever image is submitted. The
whole pipeline is **off by default** (`processing.enabled=false`), so every submission
is moderated by hand until it is switched on. Auto-approval
(`review.auto-approve.enabled`) is likewise off; mark creation is never automatic.

## Direction

- A public catalogue UI with richer browsing and media.
- A reproducible database schema (migrations and constraints) and deployment.
- Better matching (rotation handling, or a trained embedder) only if the descriptor
  proves insufficient in practice.
