# Kế hoạch triển khai Redis Blacklist cho JWT

## 1. Kiến trúc

```
┌──────────┐    1. Gửi JWT          ┌──────────────────┐
│ Frontend │ ─────────────────────▶  │  JwtAuthFilter    │
│ (Vue 3)  │   Authorization Bearer  │                   │
│          │                         │  2. Check Redis   │
│          │                         │  ─────▶ ┌────────┐│
│          │   3. POST /auth/logout  │         │ Redis  ││
│          │ ─────────────────────▶  │         └────────┘│
│          │   { refreshToken }      │                   │
│          │                         │  4. Blacklist jti │
│          │                         │  ─────▶ ┌────────┐│
│          │                         │         │ Redis  ││
└──────────┘                         └────────┘──────────┘
```

**Luồng chi tiết:**

1. Khi tạo JWT (access/refresh), thêm `jti` (JWT ID) = UUID
2. Khi request đến, `JwtAuthFilter` decode token → lấy `jti` → check Redis
3. Nếu `jti` có trong Redis → token đã bị blacklist → 401
4. Logout: FE gửi access token (header) + refresh token (body) → BE blacklist cả 2
5. Refresh token rotation: blacklist refresh token cũ, cấp mới
6. Redis TTL = thời gian còn lại của token → tự động xoá khi hết hạn

---

## 2. INPUT / OUTPUT

### Logout (thay đổi)
```
POST /api/auth/logout
Headers: Authorization: Bearer <accessToken>
Body:    { "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
Response (200): { "message": "Logged out successfully" }
```

### Request bị từ chối vì token đã blacklist
```
GET /api/devices
Headers: Authorization: Bearer <blacklistedToken>
Response (401): { "message": "Token has been revoked" }
```

---

## 3. NHỮNG FILE CẦN THAY ĐỔI / THÊM

### Backend (10 files)

| # | File | Hành động | Mô tả |
|---|------|-----------|-------|
| 1 | `pom.xml` | ✏️ Thêm | `spring-boot-starter-data-redis` |
| 2 | `docker/docker-compose.yml` | ✏️ Thêm | Redis service |
| 3 | `application.properties` | ✏️ Sửa | Thêm Redis host/port |
| 4 | `service/JwtService.java` | ✏️ Sửa | Thêm `jti` claim, `extractJti()` |
| 5 | `service/TokenBlacklistService.java` | **➕ Mới** | Redis blacklist ops |
| 6 | `filter/JwtAuthFilter.java` | ✏️ Sửa | Check blacklist trước khi set auth |
| 7 | `controller/AuthController.java` | ✏️ Sửa | Logout nhận tokens, gọi blacklist |
| 8 | `controller/AuthController.java` | ✏️ Sửa | Thêm `LogoutRequest` DTO |
| 9 | `service/AuthService.java` | ✏️ Sửa | Thêm `logout()` method |
| 10 | `config/SecurityConfig.java` | ✏️ Sửa | Require auth cho `/api/auth/logout` |

### Frontend (3 files)

| # | File | Thay đổi |
|---|------|----------|
| 1 | `api/auth.api.ts` | `logout()` gửi refreshToken trong body |
| 2 | `stores/auth.store.ts` | `logout()` gọi api kèm refreshToken |
| 3 | `utils.ts` | Thêm type `LogoutRequest` |

### IoT Simulator
| File | Thay đổi |
|------|----------|
| Không cần | Dùng MQTT, không dùng REST auth |

---

## 4. CÁCH IMPLEMENT

### 4.1 Docker — Thêm Redis service

**`docker/docker-compose.yml`**
```yaml
services:
  mosquitto:
    # ... không đổi

  postgres:
    # ... không đổi

  redis:
    image: redis:7-alpine
    container_name: iot-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    restart: unless-stopped

volumes:
  mosquitto-data:
  postgres-data:
  redis-data:        # ← thêm
```

### 4.2 Backend — Thêm dependency

**`pom.xml`**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 4.3 Backend — Cấu hình Redis

**`application.properties`**
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 4.4 Backend — JwtService: thêm JTI

**`service/JwtService.java`** — Thêm vào `generateToken` và `generateRefreshToken`:
```java
import java.util.UUID;

public String generateToken(String username) {
    return Jwts.builder()
            .subject(username)
            .id(UUID.randomUUID().toString())    // ← jti
            .claim("type", "access")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
}

// Tương tự cho generateRefreshToken

public String extractJti(String token) {
    return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getId();
}

public long getRemainingExpiration(String token) {
    Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    return claims.getExpiration().getTime() - System.currentTimeMillis();
}
```

### 4.5 Backend — TokenBlacklistService (MỚI)

**`service/TokenBlacklistService.java`**
```java
package com.globalict.iot_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public void blacklistToken(String token) {
        if (!jwtService.isTokenValid(token) && !jwtService.isRefreshTokenValid(token)) {
            return;
        }
        String jti = jwtService.extractJti(token);
        long ttl = jwtService.getRemainingExpiration(token);
        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti, "revoked", ttl, TimeUnit.MILLISECONDS
            );
        }
    }

    public boolean isBlacklisted(String token) {
        String jti = jwtService.extractJti(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
```

### 4.6 Backend — JwtAuthFilter: check blacklist

**`filter/JwtAuthFilter.java`** — Thêm `TokenBlacklistService` dependency, check sau `isTokenValid`:
```java
// Sau dòng: if (!jwtService.isTokenValid(token)) {
if (!jwtService.isTokenValid(token) || tokenBlacklistService.isBlacklisted(token)) {
    log.warn("Token is invalid or blacklisted");
    filterChain.doFilter(request, response);
    return;
}
```

### 4.7 Backend — AuthController / AuthService: logout thực tế

**`Dto/LogoutRequest.java`** (MỚI)
```java
@Data
public class LogoutRequest {
    private String refreshToken;
}
```

**`service/AuthService.java`** — thêm method:
```java
public void logout(String accessToken, String refreshToken) {
    if (accessToken != null) {
        tokenBlacklistService.blacklistToken(accessToken);
    }
    if (refreshToken != null) {
        tokenBlacklistService.blacklistToken(refreshToken);
    }
    log.info("User logged out, tokens blacklisted");
}
```

**`controller/AuthController.java`** — sửa logout:
```java
@PostMapping("/logout")
public ResponseEntity<Map<String, String>> logout(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody(required = false) LogoutRequest request) {
    String accessToken = authHeader != null && authHeader.startsWith("Bearer ")
            ? authHeader.substring(7) : null;
    String refreshToken = request != null ? request.getRefreshToken() : null;
    authService.logout(accessToken, refreshToken);
    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
}
```

**`config/SecurityConfig.java`** — cần auth cho `/logout`:
```java
.requestMatchers("/api/auth/login").permitAll()
.requestMatchers("/api/auth/register").permitAll()
.requestMatchers("/api/auth/refresh").permitAll()
.requestMatchers("/api/auth/me").authenticated()   // ← thêm
.requestMatchers("/api/auth/logout").authenticated() // ← thêm
.requestMatchers("/ws/**").permitAll()
```

### 4.8 Frontend — Cập nhật

**`utils.ts`**
```typescript
export interface LogoutRequest {
  refreshToken: string
}
```

**`api/auth.api.ts`**
```typescript
logout: (data: LogoutRequest) =>
    http.post('/auth/logout', data),
```

**`stores/auth.store.ts`**
```typescript
async function logout() {
  try {
    if (refreshToken.value) {
      await authApi.logout({ refreshToken: refreshToken.value })
    }
  } catch {
    // ignore — vẫn logout local dù api fail
  } finally {
    accessToken.value = null
    refreshToken.value = null
    username.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    router.push('/login')
  }
}
```

---

## 5. KIỂM TRA

### 5.1 Backend

| Test case | Steps | Expected |
|-----------|-------|----------|
| Logout → token bị blacklist | 1. Login → lấy token<br>2. Logout<br>3. Gọi API với token cũ | 401 "Token has been revoked" |
| Token mới sau logout vẫn valid | 1. Login → lấy token<br>2. Logout<br>3. Login lại<br>4. Gọi API với token mới | 200 |
| Access token hết hạn → không cần blacklist | 1. Tạo token, để nó hết hạn<br>2. Gọi API | 401 (hết hạn) |
| Refresh token rotation → token cũ invalid | 1. Login → get refreshToken<br>2. Refresh → get new tokens<br>3. Gọi refresh với token cũ | 401 |
| Concurrent requests → token invalid | Gửi request với token đã blacklist | 401 (không ảnh hưởng tới request khác) |

### 5.2 Frontend

| Test case | Steps | Expected |
|-----------|-------|----------|
| Logout gửi refresh token | Click logout | request body có `{ refreshToken: "..." }` |
| Sau logout, token cũ không dùng được | Thử gọi API bằng tay | 401 (server reject) |

### 5.3 Cách chạy

```bash
# Khởi động Redis
cd docker && docker compose up -d redis

# Backend
cd iot-backend && ./mvnw test

# Frontend
cd frontend && npm run build
```

---

## 6. SO SÁNH VỚI TOKEN VERSION

| Tiêu chí | Token Version (DB) | Redis Blacklist |
|----------|-------------------|-----------------|
| Mỗi request query | DB (I/O chậm) | Redis (RAM, ~1ms) |
| Selective revoke | ❌ — tất cả token của user | ✅ — từng token riêng |
| Cần thêm infra | ❌ | ✅ Redis |
| Dọn dẹp tự động | ❌ — phải migration | ✅ — Redis TTL |
| Độ phức tạp | Thấp | Trung bình |

---

## 7. TỔNG KẾT

| Thành phần | Hành động | File |
|------------|-----------|------|
| Docker | Thêm Redis service | `docker-compose.yml` |
| Backend dep | Thêm `spring-boot-starter-data-redis` | `pom.xml` |
| Backend config | Thêm Redis host/port | `application.properties` |
| Backend mới | `TokenBlacklistService` | check/blacklist token trong Redis |
| Backend sửa | `JwtService` | thêm UUID jti cho mỗi token |
| Backend sửa | `JwtAuthFilter` | kiểm tra blacklist |
| Backend sửa | `AuthController` + `AuthService` | logout blacklist cả 2 token |
| Backend sửa | `SecurityConfig` | logout + me require auth |
| Frontend | `auth.api.ts` + `auth.store.ts` | gửi refreshToken khi logout |
