# ProxiSpeak Backend (Member 2: Authentication, Rooms, Saved Layouts)

Spring Boot 3.3 · Java 17 · Spring Security + JWT · Spring Data MongoDB

## 1. Run it (STS or terminal)

1. Start MongoDB (local `mongod` on port 27017, or use a MongoDB Atlas connection string).
2. STS: **File > Import > Maven > Existing Maven Projects**, select this folder, then right-click
   `ProxiSpeakApplication.java` > **Run As > Spring Boot App**.
   Terminal: `mvn spring-boot:run`.
3. Server runs at `http://localhost:8080`. Check `GET /api/health`.

Environment variables (all optional for local use):

| Variable | Default | Purpose |
| --- | --- | --- |
| `MONGODB_URI` | `mongodb://localhost:27017/proxispeak` | Local MongoDB or Atlas `mongodb+srv://...` URI |
| `JWT_SECRET` | dev placeholder | Min 32 chars. **Share the same value with Member 3** |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Member 1's frontend origins |

Use MongoDB Compass to view the `proxispeak` database (collections are created automatically on first use).

## 2. Database design (MongoDB collections)

| Collection | Fields |
| --- | --- |
| `users` | _id, username (unique), email (unique), passwordHash (BCrypt), createdAt |
| `rooms` | _id, roomCode (unique, 6 chars), name, ownerId, ownerUsername, maxUsers, memberIds [array of user ids], createdAt |
| `office_layouts` | _id, roomId (unique, one layout per room), name, layoutJson, updatedAt |

Design note: room membership is stored as an embedded `memberIds` array inside the room document, so
checking membership and counting members needs no join. Unique indexes are created automatically
(`spring.data.mongodb.auto-index-creation=true`).

## 3. REST API

All endpoints except register/login/health need `Authorization: Bearer <token>`.
Errors are always `{ "status": 409, "error": "Conflict", "message": "Username already taken" }`.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register` | `{username,email,password}` -> `{token,user}` (201) |
| POST | `/api/auth/login` | `{username,password}` -> `{token,user}` |
| GET | `/api/auth/me` | current user |
| POST | `/api/rooms` | `{name,maxUsers?}` create room (you become owner + member) |
| GET | `/api/rooms` | rooms you own or joined |
| GET | `/api/rooms/{code}` | room details (members only) |
| POST | `/api/rooms/{code}/join` | join by code (409 if full, idempotent) |
| POST | `/api/rooms/{code}/leave` | leave (owner cannot) |
| DELETE | `/api/rooms/{code}` | delete room + layout (owner only) |
| GET | `/api/rooms/{code}/validate` | for Member 3: may this token's user enter this room? |
| GET | `/api/rooms/{code}/layout` | load saved layout (members; 404 if none) |
| PUT | `/api/rooms/{code}/layout` | `{name?,layoutJson}` save layout (owner only, must be valid JSON) |

`layoutJson` is a JSON **string**; its internal structure (walls, desks, spawn points) is agreed with Member 1.

### Quick test (Postman or curl)

```bash
curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"akash","email":"akash@example.com","password":"secret123"}'
TOKEN=<paste token>
curl -s -X POST localhost:8080/api/rooms -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"Team Office"}'
curl -s -X PUT localhost:8080/api/rooms/ABC123/layout -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Floor 1","layoutJson":"{\"walls\":[],\"desks\":[{\"x\":100,\"y\":80}],\"spawn\":{\"x\":50,\"y\":50}}"}'
```

## 4. Integration contracts

**With Member 3 (realtime server)** - agree on: room ID = `roomCode`; JWT is HS256 signed with the shared
`JWT_SECRET`, claims `sub` = username, `uid` = user id (MongoDB ObjectId string). When a socket connects,
Member 3 can either (a) verify the JWT locally with the same secret, or (b) call
`GET /api/rooms/{code}/validate` with the user's token and accept only if `valid && !full`.

**With Member 1 (frontend)** - keep `token` in memory/localStorage, send it as a Bearer header, load the
layout with `GET /api/rooms/{code}/layout` after joining; owners save with `PUT`.

## 5. Security notes (for viva)

- Passwords hashed with BCrypt, never returned by the API.
- Stateless JWT auth (24 h expiry); CSRF disabled because no cookies/sessions are used.
- Authorization: only members can read a room/layout; only the owner can edit the layout or delete the room.
- Bean Validation on all request bodies; layout JSON validity is checked.
- Known limitation: joining a nearly-full room from two clients at the exact same instant could exceed
  `maxUsers` by one (no multi-document transaction). Fine for this project; an atomic `$addToSet` update would fix it.
- Change `JWT_SECRET` before any real deployment.

## 6. Report chapter outline (Member 2)

1. Backend architecture (controller / repository / document layers, diagram)
2. MongoDB schema (collections and relationships, why membership is embedded)
3. Authentication flow (register -> login -> JWT -> filter)
4. Room management and membership rules
5. Layout persistence
6. API reference and testing results (Postman screenshots)
7. Limitations and future work (refresh tokens, password reset, rate limiting)
