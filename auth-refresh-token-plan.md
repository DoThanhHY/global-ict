# Kế hoạch triển khai Authentication với Access Token + Refresh Token

## 1. Kiến trúc tổng quan

```
┌────────────┐   Access Token (15p)    ┌──────────────┐
│  Frontend   │ ──── Authorization ──▶ │   Backend     │
│  (Vue 3)    │     Bearer <token>     │  (Spring Boot)│
│             │◀──────── 200 OK ───────│              │
│             │                         │              │
│   Khi 401:  │                         │              │
│             │── POST /auth/refresh ──▶│              │
│             │   { refreshToken }      │              │
│             │◀── { accessToken } ─────│              │
│             │                         │              │
│   Nếu fail  │                         │              │
│   → logout  │                         │              │
└────────────┘                         └──────────────┘
```

**Luồng chi tiết:**

1. Login → nhận `accessToken` (15 phút) + `refreshToken` (7 ngày)
2. Mọi request gửi `accessToken` trong `Authorization: Bearer <token>`
3. Khi `accessToken` hết hạn → Backend trả về 401
4. Frontend tự động gọi `POST /api/auth/refresh` với `refreshToken`
5. Backend validate `refreshToken`, trả về `accessToken` mới
6. Frontend retry request gốc với `accessToken` mới
7. Nếu `refreshToken` cũng hết hạn → logout, chuyển về trang login

---

## 2. INPUT / OUTPUT

### Backend

#### Login
```
POST /api/auth/login

Request  (body): { "username": "admin", "password": "12345678" }
Response (200):  {
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

#### Refresh token
```
POST /api/auth/refresh

Request  (body): { "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
Response (200):  {
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}

Response (401):  { "message": "Invalid or expired refresh token" }
```

#### Các API còn lại (không đổi)
```
Request:  Authorization: Bearer <accessToken>
Response (khi accessToken hết hạn): 401
```

### Frontend

#### Login flow
```
Input:  { username: string, password: string }
Output: lưu accessToken + refreshToken vào localStorage
        → chuyển hướng về Dashboard
```

#### Auto-refresh flow
```
Input:  401 từ API
Process:
  1. Kiểm tra đã có refresh đang chạy chưa (tránh gọi trùng)
  2. Gọi POST /api/auth/refresh với refreshToken
  3. Nếu ok: cập nhật accessToken mới, retry request gốc
  4. Nếu fail: logout
Output: request gốc thành công hoặc redirect login
```

---

## 3. NHỮNG FILE CẦN THAY ĐỔI

### Backend (8 files)

| # | File | Thay đổi |
|---|------|----------|
| 1 | `application.properties` | Thêm `jwt.refresh-expiration=604800000` |
| 2 | `Dto/AuthResponse.java` | Đổi `token` → `accessToken`, thêm `refreshToken` |
| 3 | `service/JwtService.java` | Thêm `generateRefreshToken()`, `isRefreshTokenValid()` |
| 4 | `service/AuthService.java` | Login trả về cả 2 token, thêm `refreshAccessToken()` |
| 5 | `controller/AuthController.java` | Thêm endpoint `POST /api/auth/refresh` |
| 6 | `controller/AuthController.java` | Thêm `RefreshTokenRequest` DTO (có thể để trong cùng file) |
| 7 | `config/RateLimitingConfig.java` | Thêm `/api/auth/refresh` vào `Tier.AUTH` |
| 8 | `filter/JwtAuthFilter.java` | Không đổi (chỉ validate access token) |

### Frontend (5 files)

| # | File | Thay đổi |
|---|------|----------|
| 1 | `utils.ts` | Thêm `refreshToken` vào `AuthResponse`, thêm type `RefreshRequest` |
| 2 | `api/auth.api.ts` | Thêm hàm `refresh()` |
| 3 | `stores/auth.store.ts` | Lưu `refreshToken`, thêm `refreshTokens()`, `logout()` xoá cả 2 |
| 4 | `api/http.ts` | Response interceptor: thử refresh trước khi logout |
| 5 | `composables/useWebSocket.ts` | Khi accessToken đổi → reconnect WebSocket |

### IoT Simulator

| File | Thay đổi |
|------|----------|
| Không cần thay đổi | Simulator dùng MQTT, không dùng REST API |

---

## 4. CÁCH IMPLEMENT

### 4.1 Backend

#### Bước 1: application.properties
```properties
jwt.secret=your-super-secret-key-must-be-at-least-256-bits-long
jwt.expiration=900000            # 15 phút cho access token
jwt.refresh-expiration=604800000 # 7 ngày cho refresh token
```

#### Bước 2: JwtService.java
```java
public String generateToken(String username) {
    return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
}

public String generateRefreshToken(String username) {
    return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
            .signWith(getRefreshSigningKey()) // Có thể dùng key riêng hoặc chung
            .compact();
}

public boolean isRefreshTokenValid(String token) {
    try {
        Jwts.parser()
                .verifyWith(getRefreshSigningKey())
                .build()
                .parseSignedClaims(token);
        return true;
    } catch (Exception e) {
        log.error("Invalid refresh token: {}", e.getMessage());
        return false;
    }
}
```

> **Lưu ý:** Nên dùng 2 secret key khác nhau cho access và refresh để tăng bảo mật. Hoặc dùng chung key + claim `type` để phân biệt.

#### Bước 3: AuthResponse.java
```java
@Builder @Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String username;
    private String role;
}
```

#### Bước 4: AuthService.java
```java
public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                    request.getUsername(), request.getPassword()));
    
    User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow();
    
    String accessToken = jwtService.generateToken(user.getUsername());
    String refreshToken = jwtService.generateRefreshToken(user.getUsername());
    
    return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .username(user.getUsername())
            .role(user.getRole())
            .build();
}

public AuthResponse refreshAccessToken(String refreshToken) {
    if (!jwtService.isRefreshTokenValid(refreshToken)) {
        throw new RuntimeException("Invalid refresh token");
    }
    
    String username = jwtService.extractUsername(refreshToken);
    String newAccessToken = jwtService.generateToken(username);
    String newRefreshToken = jwtService.generateRefreshToken(username);
    
    return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
}
```

#### Bước 5: AuthController.java
```java
@PostMapping("/refresh")
public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> request) {
    String refreshToken = request.get("refreshToken");
    return ResponseEntity.ok(authService.refreshAccessToken(refreshToken));
}
```

#### Bước 6: RateLimitingConfig.java
```java
if (path.matches("/api/auth/(login|register|refresh)")) {
    return Tier.AUTH;
}
```

### 4.2 Frontend

#### Bước 1: utils.ts
```typescript
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  username: string
  role: string
}

export interface RefreshRequest {
  refreshToken: string
}
```

#### Bước 2: auth.api.ts
```typescript
export const authApi = {
  login: (data: LoginRequest) =>
    http.post<AuthResponse>('/auth/login', data).then(r => r.data),
  refresh: (refreshToken: string) =>
    http.post<AuthResponse>('/auth/refresh', { refreshToken }).then(r => r.data),
  // ...
}
```

#### Bước 3: auth.store.ts
```typescript
const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
const accessToken = ref<string | null>(localStorage.getItem('accessToken'))

async function login(data: LoginRequest) {
  const res = await authApi.login(data)
  accessToken.value = res.accessToken
  refreshToken.value = res.refreshToken
  username.value = res.username
  localStorage.setItem('accessToken', res.accessToken)
  localStorage.setItem('refreshToken', res.refreshToken)
  localStorage.setItem('username', res.username)
  router.push('/')
}

function logout() {
  accessToken.value = null
  refreshToken.value = null
  username.value = null
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('username')
  router.push('/login')
}
```

#### Bước 4: http.ts (quan trọng nhất — auto-refresh trên 401)
```typescript
import axios from 'axios'
import { useAuthStore } from '../stores/auth.store'
import { router } from '../router'

export const http = axios.create({
  baseURL: 'http://localhost:8080/api',
})

let isRefreshing = false
let failedQueue: Array<{
  resolve: (value: unknown) => void
  reject: (reason?: unknown) => void
}> = []

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

// Request interceptor
http.interceptors.request.use(config => {
  const authStore = useAuthStore()
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

// Response interceptor
http.interceptors.response.use(
  res => res,
  async error => {
    const originalRequest = error.config
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Nếu đang refresh, xếp hàng đợi
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return http(originalRequest)
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      const authStore = useAuthStore()
      
      if (!authStore.refreshToken) {
        authStore.logout()
        return Promise.reject(error)
      }
      
      try {
        const res = await authApi.refresh(authStore.refreshToken)
        authStore.accessToken = res.accessToken
        authStore.refreshToken = res.refreshToken
        localStorage.setItem('accessToken', res.accessToken)
        localStorage.setItem('refreshToken', res.refreshToken)
        
        processQueue(null, res.accessToken)
        
        originalRequest.headers.Authorization = `Bearer ${res.accessToken}`
        return http(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        authStore.logout()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }
    
    return Promise.reject(error)
  }
)
```

#### Bước 5: useWebSocket.ts
Khi access token được refresh, WebSocket cần reconnect với token mới. Có 2 hướng:
1. Đơn giản: WebSocket dùng access token (nếu token đổi → disconnect rồi reconnect)
2. Hoặc: WebSocket dùng refresh token (vì nó sống lâu hơn) — nhưng không khuyến khích

**Khuyến nghị:** WebSocket dùng access token. Khi refresh xong, gọi lại `useWebSocket()` để reconnect.

---

## 5. KIỂM TRA (Testing)

### 5.1 Backend

#### Unit test: JwtService
| Test case | Expected |
|-----------|----------|
| `generateToken()` tạo token thành công | Token không null |
| `generateRefreshToken()` tạo token thành công | Token không null |
| `extractUsername()` lấy đúng username từ token | Username khớp |
| `isTokenValid()` với token hợp lệ | `true` |
| `isTokenValid()` với token hết hạn | `false` |
| `isRefreshTokenValid()` với refresh token hết hạn | `false` |

#### Unit test: AuthService
| Test case | Expected |
|-----------|----------|
| `login()` trả về cả `accessToken` và `refreshToken` | AuthResponse có 2 token |
| `refreshAccessToken()` với refresh token hợp lệ | Trả về access token mới |
| `refreshAccessToken()` với refresh token hết hạn | Throw exception |

#### Integration test: AuthController
| Test case | Steps | Expected |
|-----------|-------|----------|
| Full flow | 1. Register user | 201 |
| | 2. Login | 200 + accessToken + refreshToken |
| | 3. Gọi API thường với accessToken | 200 |
| | 4. Refresh token | 200 + accessToken mới |
| | 5. Gọi API với accessToken mới | 200 |
| | 6. Gọi API với accessToken cũ | 401 (đã hết hạn hoặc bị thay thế) |
| Refresh không hợp lệ | Gọi refresh với token rỗng | 400 |
| | Gọi refresh với token đã hết hạn | 401 |

### 5.2 Frontend

| Test case | Steps | Expected |
|-----------|-------|----------|
| Login thành công | Nhập user/pass → submit | Lưu accessToken + refreshToken, redirect Dashboard |
| Access token hết hạn | Gọi API → 401 → gọi refresh → thành công | Retry thành công, không logout |
| Refresh token hết hạn | Gọi API → 401 → gọi refresh → 401 | Logout, redirect Login |
| Concurrent 401 | 3 request cùng lúc → 401 cả 3 | Chỉ gọi refresh 1 lần, cả 3 retry thành công |
| Logout | Click logout | Xoá cả 2 token, redirect Login |

### 5.3 Cách chạy test

```bash
# Backend
cd iot-backend
./mvnw test

# Frontend (nếu có test)
cd frontend
npm run test
```

---

## 6. BẢNG TỔNG KẾT

| Thành phần | File | Thay đổi | Mức độ |
|------------|------|----------|--------|
| Backend | `application.properties` | +1 property | Dễ |
| Backend | `AuthResponse.java` | Sửa field names + thêm field | Dễ |
| Backend | `JwtService.java` | +2 methods | Trung bình |
| Backend | `AuthService.java` | Sửa login + thêm refreshAccessToken | Trung bình |
| Backend | `AuthController.java` | +1 endpoint | Dễ |
| Backend | `RateLimitingConfig.java` | +1 path match | Dễ |
| Frontend | `utils.ts` | Sửa type | Dễ |
| Frontend | `auth.api.ts` | +1 function | Dễ |
| Frontend | `auth.store.ts` | Sửa lưu token + thêm hàm | Trung bình |
| Frontend | `http.ts` | Sửa interceptor | Khó |
| Frontend | `useWebSocket.ts` | Reconnect logic | Trung bình |

**Tổng số file cần thay đổi:** 11 (6 backend + 5 frontend)
**Thời gian ước tính:** 1-2 ngày (tuỳ kinh nghiệm)
