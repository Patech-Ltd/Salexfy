# Salexfy POS Multi-Device Sync — Backend API

This document defines the HTTP contract the Android app expects from the sync
backend (Spring Boot). The Android client is implemented against this contract
in `com.patechltd.salexfypos.sync` (SyncManager/SyncApplier).

## Conventions

- Base URL: configured on the device (`Settings → Multi-device Sync → Server URL`).
  Trailing slashes are stripped.
- All endpoints return `application/json` and require `Authorization: Bearer <token>`.
- Content type for bodies is `application/json`.
- Time values are **milliseconds since Unix epoch** (UTC).
- The backend must keep a single global, append-only **change log** shared by all
  devices (a single `sync_changes`-style table). Each row is an immutable change
  record; the sequence number (`seq`) is the cursor for incremental pulls.

## Authentication

### `POST /api/auth/login`

Body:

```json
{ "username": "user", "password": "pass" }
```

The client accepts any of the following response shapes (checked in order):

```json
{ "token": "<jwt>" }
{ "accessToken": "<jwt>" }
{ "data": { "token": "<jwt>" } }
```

If no token is found the login is treated as failed.

## Push changes to the server

### `POST /api/sync/push`

Body:

```json
{
  "deviceId": "android-<uuid>",
  "changes": [
    {
      "seq": 42,
      "entityType": "PRODUCT",
      "recordId": "<entity uid>",
      "operation": "INSERT",
      "payload": "{ \"uid\": \"...\", \"name\": \"...\" }",
      "updatedAt": 1723800000000
    }
  ]
}
```

- `seq` is the client's local sequence number (monotonic per device).
- `operation` is one of `INSERT`, `UPDATE`, `DELETE`. (The client currently emits
  `INSERT` on upsert; the server should treat `INSERT` and `UPDATE` identically —
  upsert by `recordId`.)
- `payload` is the serialized entity as JSON (see “Payload format” below). It is
  empty for `DELETE`.

Server responsibilities:

1. For each received change, persist it into the global change log **only if it is
   not a duplicate**. A change is a duplicate when the same `deviceId` +
   `seq` (or `entityType` + `recordId` + `updatedAt`) already exists.
2. Apply each change to the shared database (upsert for INSERT/UPDATE, delete for
   DELETE), using last-write-wins: the change with the **largest `updatedAt`**
   wins when a record was modified by two devices.
3. Persist the server’s own `seq` for the change (the global cursor), so other
   devices can pull it.

Success response — the server MUST include the `acknowledged` array of the
client `seq` values it accepted:

```json
{ "acknowledged": [42, 43, 44] }
```

If `acknowledged` is missing the client treats the whole push as failed (it never
marks anything synced). The client marks the acknowledged `seq`s as synced and
keeps at most the newest 5000 synced rows locally.

## Pull changes from the server

### `GET /api/sync/pull?since=<ms>&deviceId=<device-id>`

- `since` — client’s last successful pull cursor. The client derives it from its
  own log: the max `timestamp` of a successful `PULL`. It is `0` on first sync.
- `deviceId` — the requesting device id (the server may use this to avoid echoing
  a device’s own changes back to it, though the client also ignores echoes via
  its mute flag).

Success response:

```json
{
  "changes": [
    {
      "seq": 100,
      "entityType": "PRODUCT",
      "recordId": "<entity uid>",
      "operation": "INSERT",
      "payload": "{ ... }",
      "updatedAt": 1723800000000
    }
  ]
}
```

Semantics the client expects:

- Only changes newer than `since` (by the global `seq` or by `updatedAt`).
- Ordered oldest → newest.
- `changes` may be absent or empty when there is nothing new.
- Each change is applied with last-write-wins by `updatedAt`; the client applies
  them muted so they are never re-pushed.

## `entityType` values

The client tracks these entity types:

`PRODUCT`, `PRODUCT_BARCODE`, `CATEGORY`, `BRAND`, `UNIT`, `SUPPLIER`,
`CUSTOMER`, `DEBT_PAYMENT`, `PURCHASE`, `PURCHASE_ITEM`, `SALE`, `SALE_ITEM`,
`SALE_PAYMENT`, `STOCK_MOVEMENT`, `STOCK_TAKE`, `USER`, `ROLE`, `EXPENSE`.

`STOCK_TAKE` items (`stock_take_items`) are intentionally **not** synced.

## Payload format

Payloads are flat JSON of the entity’s public fields, e.g. a `Product`:

```json
{
  "uid": "p-001",
  "name": "Milo 1kg",
  "categoryId": "cat-2",
  "barcode": "123456789",
  "cost": 250.0,
  "price": 310.0,
  "stockQty": 15.0,
  "minStock": 5.0,
  "imagePath": "",
  "createdAt": 1723800000000
}
```

Field names match the Room entities. Nulls are omitted; missing fields decode to
defaults (0 / false / "").

## Server storage recommendations

- **Global change log table** `sync_changes`:
  `seq` (PK, autoincrement), `deviceId`, `entityType`, `recordId`, `operation`,
  `payload` (TEXT), `updatedAt` (BIGINT).
- Index `entityType`, `recordId`, `updatedAt`, and `seq`.
- **Shared tables** mirroring the Android entities with the same column names, so
  `payload` can be upserted directly.
- Deterministic conflict resolution: compare `updatedAt` on upsert;
  `INSERT OR IGNORE` semantics for the change log to dedupe re-pushes.

## Error handling

- Non-2xx responses (with or without a body) are treated as failures; the client
  logs the HTTP status and body into its sync log and leaves the changes unsynced
  for the next attempt.
- The client does not currently implement retry backoff beyond the periodic sync
  worker; keep failures idempotent server-side.
