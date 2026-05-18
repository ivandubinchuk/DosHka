# Доповнення до специфікації v1.1
# Нові технології, брутальний дизайн, розширена функціональність

---

## 1. НАЗВА ПРОДУКТУ

**DoshKa** — Канбан-дошка в кишені. Просто, впізнаванно, по-нашому.
Слоган: «Постав. Контролюй. Закрий.»

---

## 2. РОЗШИРЕНИЙ ТЕХНОЛОГІЧНИЙ СТЕК

### 2.1. Сервер — нові залежності

| Технологія | Замість / Додатково | Навіщо |
|---|---|---|
| **Pydantic v2 + msgspec** | Стандартна серіалізація | msgspec для швидкої серіалізації WebSocket-повідомлень (у 5-10 разів швидше json.dumps) |
| **Passlib[bcrypt] + argon2-cffi** | Тільки bcrypt | Argon2id як основний алгоритм хешування паролів (переможець PHC), bcrypt як fallback |
| **python-multipart + Pillow** | — | Обробка завантажених файлів, генерація thumbnail для зображень-вкладень |
| **WeasyPrint** | — | Генерація PDF-звітів із HTML-шаблонів (графіки, таблиці, стилізація під бренд) |
| **openpyxl** | — | Генерація Excel-звітів із форматуванням, формулами, діаграмами |
| **Jinja2** | — | Шаблони для email-листів (скидання пароля, запрошення) та HTML-звітів |
| **python-jose[cryptography]** | — | JWT із підтримкою RS256/ES256 (асиметричні ключі) для продакшну |
| **slowapi** | Власний rate limiter | Декларативний rate limiting на основі стандарту |
| **structlog** | logging | Структуроване логування в JSON-форматі для зручного парсингу |
| **sentry-sdk[fastapi]** | — | Відстеження помилок у продакшні, performance monitoring |
| **httpx** | — | Async HTTP-клієнт для виклику FCM API, відправки email через API (SendGrid/Mailgun) |
| **cryptography** | — | Шифрування файлу БД (SQLCipher через sqlcipher3), підпис токенів |
| **qrcode + segno** | — | Генерація QR-кодів для швидкого запрошення виконавців у команду |

### 2.2. Клієнт (Kotlin) — розширений стек

| Технологія | Навіщо |
|---|---|
| **Jetpack Compose 1.6+ (Material3)** | Останні Compose API: shared element transitions, predictive back, LargeTopAppBar |
| **Compose Navigation 2.8+** | Type-safe navigation з аргументами (Kotlin Serialization) |
| **Accompanist** | SystemUiController (прозорий статус-бар), Permissions API |
| **Coil 3** | Async завантаження зображень (аватари, вкладення, thumbnails), кешування, placeholder/error |
| **Lottie Compose** | Анімації: splash-screen, порожні стани, успішні дії, drag-and-drop feedback |
| **Vico / MPAndroidChart Compose** | Графіки дашборду: лінійні, кругові, гістограми з анімацією |
| **BiometricPrompt (AndroidX Biometric)** | Вхід по відбитку пальця / Face Unlock / iris scan |
| **AndroidX Security (EncryptedSharedPreferences)** | Шифроване зберігання JWT-токенів та sensitive даних на пристрої |
| **Kotlin Serialization** | JSON-серіалізація замість Gson (швидше, type-safe, multiplatform-ready) |
| **DataStore Proto** | Типізоване зберігання налаштувань (протобуфери замість key-value) |
| **WorkManager** | Фонова синхронізація pending операцій при відновленні мережі (surviving process death) |
| **Timber** | Логування з тегами та деревами (debug vs release конфігурація) |
| **LeakCanary** | Автоматичне виявлення memory leaks у debug-збірках |
| **Compose UI Testing + Robolectric** | Скріншот-тестування компонентів без емулятора |
| **KSP (Kotlin Symbol Processing)** | Швидша генерація коду для Hilt, Room замість kapt |
| **Baseline Profiles** | Прекомпіляція критичних шляхів (startup, Канбан-скрол) для швидшого запуску |
| **App Startup** | Ініціалізація компонентів (Timber, Coil, Hilt) без ContentProvider overhead |

---

## 3. БІОМЕТРИЧНА АУТЕНТИФІКАЦІЯ

### 3.1. Сценарій входу (повний)

```
Перший вхід:
  1. Email + пароль → сервер повертає access + refresh токени
  2. Клієнт пропонує: «Увімкнути вхід по відбитку пальця?»
  3. Якщо так → refresh-токен шифрується через AndroidX Security
     (EncryptedSharedPreferences, ключ зберігається в Android Keystore,
     прив'язаний до біометрії через setUserAuthenticationRequired(true))
  4. При наступному відкритті додатку:
     - BiometricPrompt (BIOMETRIC_STRONG, Class 3) → розшифровка refresh-токена → автооновлення access-токена → вхід без паролю
  5. Fallback: якщо біометрія недоступна (зламаний сканер, нові відбитки) → форма email+пароль

Повторний вхід (біометрія увімкнена):
  Splash screen → BiometricPrompt → 0.3с → головний екран
  (користувач навіть не бачить форму логіна)
```

### 3.2. Рівні аутентифікації

| Рівень | Коли | Механізм |
|---|---|---|
| **Стандартний** | Звичайні дії (перегляд, переміщення) | Валідний access-токен |
| **Підвищений** | Видалення задачі, зміна виконавця, генерація звіту | Біометрія або повторне введення пароля (step-up auth) |
| **Критичний** | Зміна пароля, деактивація користувача, видалення команди | Обов'язкове введення поточного пароля |

### 3.3. Технічні деталі

- Android Keystore: ключ RSA-2048, bound to biometric (invalidated при зміні біометрії)
- CryptoObject передається в BiometricPrompt для підтвердження, що саме цей ключ розблоковано
- Підтримка: відбиток пальця (FINGERPRINT), розпізнавання обличчя (FACE), сканер сітківки (IRIS) — все що пристрій підтримує через BIOMETRIC_STRONG
- Fallback на device credentials (PIN/pattern) — опціонально, конфігурується

---

## 4. БРУТАЛЬНИЙ ДИЗАЙН (INDUSTRIAL / NEO-BRUTALISM)

### 4.1. Концепція

Стиль: **Industrial Neo-Brutalism** — грубі форми, великі шрифти, жорсткі тіні,
контрастні кольори, мінімум прикрас, максимум функції. Інтерфейс як командний
центр — кожен елемент на місці, нічого зайвого, все під рукою.

### 4.2. Кольорова палітра

```
Фон (Surface):
  Світла тема:  #F5F0EB (теплий бетон)
  Темна тема:   #1A1A1A (вугілля)

Primary:        #FF5722 (палаюча сталь — Deep Orange 600)
Primary Dark:   #D84315 (розпечений метал)
Secondary:      #FFB300 (іскри — Amber 700)
Surface Card:   #FFFFFF / #242424 (плити, жорсткі тіні)
Error:          #FF1744 (аварія — Red A400)
Success:        #00E676 (зелене світло — Green A400)
Text Primary:   #1A1A1A / #F5F0EB
Text Secondary: #757575 / #9E9E9E
```

### 4.3. Типографіка

```
Основний шрифт:  JetBrains Mono (моноширинний — промисловий вигляд, технічність)
Заголовки:       Inter Black (800-900 weight) — масивні, впевнені
Числа/метрики:   Space Grotesk (технічний, але читабельний)

Розміри:
  H1 (екран):     32sp, Inter Black, uppercase, letter-spacing 2sp
  H2 (секція):    24sp, Inter Bold
  Body:           16sp, JetBrains Mono, line-height 1.6
  Caption:        12sp, Inter Medium, uppercase, letter-spacing 1sp
  Числа дашборду: 48sp, Space Grotesk Bold
```

### 4.4. Компоненти UI

#### Картки (Cards)
```
- Жорсткі тіні: offset (4dp, 4dp), без blur → ефект «вирізаного блоку»
- Рамка: 2dp solid чорна (світла тема) / біла (темна тема)
- Border-radius: 0dp (прямі кути) АБО 4dp максимум
- Ніяких градієнтів, ніяких м'яких тіней
- Hover/press стан: інвертування кольорів (фон ↔ текст)
```

#### Кнопки
```
- Прямокутні, жирний border 2-3dp
- Uppercase text, letter-spacing 1-2sp
- Жорстка тінь (зміщення 3dp)
- При натисканні: тінь зникає, кнопка «вдавлюється» (offset 0)
- Primary: фон #FF5722, текст білий, тінь чорна
- Secondary: фон прозорий, border чорний, текст чорний
- Destructive: фон #FF1744, border 3dp чорний
```

#### Канбан-картка задачі
```
┌─────────────────────────────────┐░░
│ ■ TEAM-042         🔴 CRITICAL │░░  ← пріоритет як штамп
│                                 │░░
│ ПІДГОТУВАТИ ЗВІТ ЗА Q4        │░░  ← заголовок великими
│                                 │░░
│ ┌──────┐  Іванов О.            │░░
│ │avatar│  до 15.02 ⚠ OVERDUE  │░░  ← червоний штамп
│ └──────┘                        │░░
│                                 │░░
│ 💬 3   📎 2   🏷 #звіт #Q4     │░░  ← метрики внизу
└─────────────────────────────────┘░░
 ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  ← жорстка тінь 4dp

Пріоритет — кольорова вертикальна смуга зліва (8dp):
  critical: #FF1744 (пульсує)
  high:     #FF5722
  medium:   #FFB300
  low:      #9E9E9E
```

#### Колонка Канбан-дошки
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  В РОБОТІ          3/5 WIP      ┃  ← заголовок uppercase, моно
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃  [картка]                        ┃
┃  [картка]                        ┃
┃  [картка]                        ┃
┃                                  ┃
┃  ░░░░░░░░░░░░░░░░░░░░░░░░░░░   ┃  ← drop zone (штрихована область)
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

WIP-прогрес: товстий progress-bar під заголовком
  Зелений → Жовтий → Червоний (при досягненні ліміту)
```

#### Дашборд
```
Великі числові блоки (плитки):
┌──────────────┐░░  ┌──────────────┐░░
│              │░░  │              │░░
│     47       │░░  │      5       │░░
│  УСЬОГО      │░░  │  ПРОСТРОЧЕНО │░░  ← червоний фон, пульсація
│              │░░  │              │░░
└──────────────┘░░  └──────────────┘░░

Графіки: мінімалістичні, без сітки, жирні лінії (3-4dp),
точки-маркери — квадратні (не круглі), кольори палітри.
Осі — JetBrains Mono, uppercase.
```

#### Чат
```
Повідомлення менеджера — справа, фон #FF5722, білий текст, прямі кути
Повідомлення виконавця — зліва, фон #242424 (або #F0F0F0), прямі кути
Системні повідомлення — центр, моноширинний шрифт, uppercase,
  стилізація під лог-запис: [12:34:05] СТАТУС ЗМІНЕНО → В РОБОТІ

Поле введення: чорна рамка 2dp, моноширинний шрифт, placeholder
  у вигляді: > введіть повідомлення...
```

### 4.5. Анімації (мінімум, але з характером)

```
- Перехід між екранами: жорсткий slide (без easing, linear 200ms)
- Drag-and-drop картки: картка «відривається» (scale 1.05, тінь збільшується до 8dp),
  при відпусканні — різкий snap на місце (spring з високим stiffness)
- Пріоритет critical: пульсація бордюру (1с цикл, ease-in-out)
- Прострочений дедлайн: червоний штамп «OVERDUE» з'являється з ефектом друку
  (друкарська машинка, по одній літері за 30ms)
- Splash screen: Lottie-анімація — три колонки збираються, картки встають на місця → назва DoshKa
- Pull-to-refresh: іконка шестерні, що обертається
- Порожній стан (немає задач): ASCII-арт порожньої дошки або «Все виконано. Відпочивай.»
- Успішна дія: короткий haptic feedback (VibrationEffect.createOneShot, 50ms)
- Видалення: картка «стискається» горизонтально і зникає (200ms)
```

### 4.6. Іконки та графіка

```
- Стиль іконок: Phosphor Icons (Bold weight) — геометричні, чіткі
  Альтернатива: Material Symbols (weight 700, grade 200)
- Без скруглень, без filled стилів — тільки outlined bold або sharp
- Пріоритет через штампи (stamps): іконки прямокутні, нахилені 5-10°,
  стилізовані під гумові печатки
- Порожні стани: мінімалістичні ілюстрації в стилі blueprint/креслення
- Аватари: квадратні (не круглі), з рамкою 2dp, fallback — ініціали
  великими літерами на контрастному фоні
```

### 4.7. Splash Screen + App Icon

```
App Icon:
  Фон: #FF5722 (або чорний)
  Символ: стилізована літера «D» із елементами Канбан-колонок або квадратна дошка з трьома вертикальними смугами
  Форма: квадрат із мінімальним скругленням (adaptive icon)

Splash Screen (Android 12+ SplashScreen API):
  Фон: #1A1A1A
  Центр: Lottie — три колонки-смуги з'являються зліва направо, картки падають зверху на місця → «DOSHKA»
  Тривалість: 1.5с (або до завершення ініціалізації)
```

---

## 5. ДОДАТКОВІ ФУНКЦІОНАЛЬНІ РОЗШИРЕННЯ

### 5.1. Швидке запрошення виконавця через QR

```
FR-QR-001: Менеджер генерує QR-код запрошення (містить: team_id + одноразовий
invite_token, термін дії 24 години). Виконавець сканує QR камерою →
відкривається екран реєстрації з попередньо заповненим team_id.
Не потрібно диктувати email чи вводити коди.
```

### 5.2. Offline-first розширення

```
FR-OFFLINE-001: При втраті з'єднання у верхній частині екрана з'являється
  жовта плашка «OFFLINE MODE» (uppercase, моноширинний) із таймером
  з моменту втрати зв'язку.

FR-OFFLINE-002: Виконавець може змінювати статус задачі в офлайні.
  Операції потрапляють у чергу (Room таблиця pending_operations).
  При відновленні з'єднання WorkManager виконує синхронізацію
  в порядку черги. Конфлікти вирішуються стратегією server-wins
  із повідомленням користувачу.
```

### 5.3. Голосові нотатки до задачі

```
FR-VOICE-001: Виконавець або менеджер може записати голосову нотатку
  (до 2 хвилин) і прикріпити її до задачі або відправити в чат.
  Формат: OGG Opus. Відтворення — інлайн-плеєр у стрічці чату.
  Використовується MediaRecorder API (Android).
```

### 5.4. Швидкі дії (Quick Actions)

```
FR-QUICK-001: Довге натискання на картку задачі відкриває контекстне меню:
  - Змінити пріоритет (вибір із 4 варіантів)
  - Призначити виконавця (список)
  - Перемістити в колонку (список)
  - Написати повідомлення (відкриває чат)
  - Видалити (менеджер)

FR-QUICK-002: Android App Shortcuts (довге натискання на іконку додатка):
  - «Створити задачу» → одразу форма створення
  - «Мої задачі» → фільтрований список
  - «Прострочені» → фільтр по overdue

FR-QUICK-003: Android Widget (домашній екран):
  - Compact: кількість задач по статусах (4 числа)
  - Expanded: список прострочених задач із кнопкою переходу
  Оновлення через WorkManager кожні 15 хвилин.
```

### 5.5. Розширена аналітика

```
FR-ANALYTICS-001: Velocity (швидкість команди): кількість завершених
  задач за спринт/тиждень. Графік тренду за останні 8 тижнів.

FR-ANALYTICS-002: Cycle Time: середній час від «В роботі» до
  «Завершено» по кожному виконавцю. Scatter plot.

FR-ANALYTICS-003: Cumulative Flow Diagram: кількість задач у кожному
  статусі по днях. Стековий area chart за обраний період.

FR-ANALYTICS-004: Heatmap активності: матриця «виконавець × день тижня»,
  інтенсивність = кількість дій. Стиль GitHub contribution graph.

FR-ANALYTICS-005: Burndown chart: залишок задач до кінця періоду
  (ідеальна лінія vs реальна).
```

### 5.6. Шаблони задач

```
FR-TEMPLATE-001: Менеджер може зберегти задачу як шаблон (title, description,
  priority, tags, estimated_hours). При створенні нової задачі — вибір
  із списку шаблонів. Шаблони зберігаються в таблиці task_templates.

FR-TEMPLATE-002: Повторювані задачі: менеджер вказує CRON-розклад
  (щоденно / щотижня / щомісяця / custom), APScheduler автоматично
  створює задачі з шаблону.
```

### 5.7. Система мітків та кольорових маркерів

```
FR-LABEL-001: Окрім тегів (текстових), менеджер може створити кольорові
  мітки (label) із назвою: наприклад, 🟥 «Баг», 🟦 «Фіча», 🟩 «Документація»,
  🟨 «Терміново». Мітки відображаються на картці як кольорові плашки.
  Фільтрація по мітках на дошці.
```

### 5.8. Експорт та інтеграції

```
FR-EXPORT-001: Експорт усіх задач команди в CSV (для подальшої обробки
  в Excel / Google Sheets).

FR-EXPORT-002: Share task — відправка деталей задачі через Android
  ShareSheet (текстовий формат із заголовком, описом, статусом, дедлайном).

FR-WEBHOOK-001: Менеджер може налаштувати webhook URL, куди сервер
  надсилатиме POST-запити при подіях (створення задачі, зміна статусу,
  завершення). Формат payload — JSON. Для інтеграції з Telegram-ботами,
  Slack, Google Sheets і т.д.
```

---

## 6. ОНОВЛЕНІ СЕРВЕРНІ ЗАЛЕЖНОСТІ (requirements.txt)

```
# Ядро
fastapi>=0.110.0
uvicorn[standard]>=0.27.0
pydantic>=2.6.0
pydantic-settings>=2.1.0

# БД
sqlalchemy[asyncio]>=2.0.25
aiosqlite>=0.19.0
alembic>=1.13.0

# Аутентифікація та безпека
python-jose[cryptography]>=3.3.0
passlib[bcrypt]>=1.7.4
argon2-cffi>=23.1.0

# Планувальник
apscheduler>=3.10.4

# HTTP / Email
httpx>=0.27.0
python-multipart>=0.0.6
jinja2>=3.1.3

# Звіти
weasyprint>=61.0
openpyxl>=3.1.2

# Зображення
pillow>=10.2.0

# QR
segno>=1.6.0

# Rate limiting
slowapi>=0.1.9

# Логування та моніторинг
structlog>=24.1.0
sentry-sdk[fastapi]>=1.40.0

# Тестування
pytest>=8.0.0
pytest-asyncio>=0.23.0
httpx  # також для тестів
factory-boy>=3.3.0
```

---

## 7. ОНОВЛЕНІ КЛІЄНТСЬКІ ЗАЛЕЖНОСТІ (build.gradle.kts)

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.8.2")

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.0")

// Lifecycle + ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// DI
implementation("com.google.dagger:hilt-android:2.50")
ksp("com.google.dagger:hilt-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

// Network
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

// Local DB
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// DataStore
implementation("androidx.datastore:datastore:1.0.0")
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Biometric
implementation("androidx.biometric:biometric:1.2.0-alpha05")

// Security (encrypted storage)
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// Images
implementation("io.coil-kt:coil-compose:3.0.0")

// Animations
implementation("com.airbnb.android:lottie-compose:6.3.0")

// Charts
implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")

// Icons
implementation("com.github.nicholasgasior:phosphor-compose:1.0.0")

// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-messaging-ktx")

// Background work
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Logging
implementation("com.jakewharton.timber:timber:5.0.1")

// Splash
implementation("androidx.core:core-splashscreen:1.0.1")

// Baseline Profiles
implementation("androidx.profileinstaller:profileinstaller:1.3.1")

// Debug
debugImplementation("com.squareup.leakcanary:leakcanary-android:2.13")
debugImplementation("androidx.compose.ui:ui-tooling")

// Testing
testImplementation("junit:junit:4.13.2")
testImplementation("io.mockk:mockk:1.13.9")
testImplementation("app.cash.turbine:turbine:1.0.0")
testImplementation("org.robolectric:robolectric:4.11.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

---

## 8. СТРУКТУРА СЕРВЕРНОГО ПРОЕКТУ (оновлена)

```
doshka-server/
├── app/
│   ├── main.py                     # FastAPI app, lifespan events, APScheduler start
│   ├── core/
│   │   ├── config.py               # pydantic-settings, .env
│   │   ├── security.py             # JWT (RS256/HS256), Argon2id хешування
│   │   ├── database.py             # aiosqlite engine, WAL mode, foreign_keys
│   │   ├── deps.py                 # FastAPI dependencies (get_db, get_current_user, require_role)
│   │   └── exceptions.py           # Кастомні exception handlers
│   ├── models/                     # SQLAlchemy ORM
│   │   ├── user.py
│   │   ├── team.py
│   │   ├── task.py
│   │   ├── board.py
│   │   ├── message.py
│   │   ├── attachment.py
│   │   ├── token.py
│   │   ├── report.py
│   │   ├── template.py             # НОВЕ: шаблони задач
│   │   ├── label.py                # НОВЕ: кольорові мітки
│   │   ├── webhook.py              # НОВЕ: webhook конфігурації
│   │   └── audit.py                # НОВЕ: audit_log
│   ├── schemas/                    # Pydantic v2
│   │   ├── auth.py
│   │   ├── user.py
│   │   ├── task.py
│   │   ├── board.py
│   │   ├── message.py
│   │   ├── analytics.py
│   │   ├── report.py
│   │   └── webhook.py
│   ├── api/v1/routers/
│   │   ├── auth.py
│   │   ├── users.py
│   │   ├── tasks.py
│   │   ├── board.py
│   │   ├── messages.py
│   │   ├── analytics.py
│   │   ├── reports.py
│   │   ├── notifications.py
│   │   ├── templates.py            # НОВЕ
│   │   ├── labels.py               # НОВЕ
│   │   └── webhooks.py             # НОВЕ
│   ├── services/
│   │   ├── auth_service.py
│   │   ├── task_service.py
│   │   ├── board_service.py
│   │   ├── message_service.py
│   │   ├── analytics_service.py
│   │   ├── report_service.py       # WeasyPrint + openpyxl
│   │   ├── notification_service.py # FCM через httpx
│   │   ├── template_service.py
│   │   ├── webhook_service.py      # Виклик зовнішніх webhooks
│   │   ├── qr_service.py           # Генерація QR-запрошень
│   │   └── thumbnail_service.py    # Pillow: resize вкладень
│   ├── repositories/
│   │   ├── task_repo.py
│   │   ├── user_repo.py
│   │   ├── message_repo.py
│   │   └── ...
│   ├── websocket/
│   │   ├── connection_manager.py   # in-memory dict user_id → ws
│   │   └── handlers.py             # Маршрутизація WS-подій
│   ├── scheduler/
│   │   ├── deadline_checker.py     # Щохвилини: перевірка наближення/прострочення
│   │   ├── cleanup.py              # Щоденно: жорстке видалення >90 днів, очистка звітів >30 днів
│   │   ├── recurring_tasks.py      # НОВЕ: створення повторюваних задач за CRON
│   │   └── backup.py               # НОВЕ: SQLite Online Backup за розкладом
│   ├── templates/                  # Jinja2 шаблони
│   │   ├── email/
│   │   │   ├── invite.html
│   │   │   ├── password_reset.html
│   │   │   └── base.html
│   │   └── reports/
│   │       ├── general.html
│   │       └── performance.html
│   └── middleware/
│       ├── cors.py
│       ├── rate_limit.py           # SlowAPI
│       ├── logging.py              # structlog middleware
│       └── sentry.py               # Sentry integration
├── alembic/
│   ├── versions/
│   └── env.py                      # batch operations для SQLite
├── tests/
│   ├── unit/
│   ├── integration/
│   └── conftest.py                 # in-memory SQLite fixtures
├── data/
│   ├── taskmanager.db              # Основна БД
│   ├── uploads/                    # Вкладення
│   └── backups/                    # Автоматичні бекапи
├── .env.example
├── requirements.txt
├── pyproject.toml                  # ruff, black, mypy конфігурація
└── README.md
```

---

## 9. НОВІ API-ЕНДПОІНТИ

| Метод | URL | Опис | Ролі |
|---|---|---|---|
| POST | /api/v1/teams/invite/qr | Генерація QR-коду запрошення | Менеджер |
| POST | /api/v1/auth/register/invite | Реєстрація за invite-токеном | Публічний |
| GET | /api/v1/templates | Список шаблонів задач | Менеджер |
| POST | /api/v1/templates | Створення шаблону | Менеджер |
| PUT | /api/v1/templates/{id} | Оновлення шаблону | Менеджер |
| DELETE | /api/v1/templates/{id} | Видалення шаблону | Менеджер |
| POST | /api/v1/tasks/from-template/{id} | Створення задачі з шаблону | Менеджер |
| GET | /api/v1/labels | Список міток команди | Всі |
| POST | /api/v1/labels | Створення мітки | Менеджер |
| DELETE | /api/v1/labels/{id} | Видалення мітки | Менеджер |
| GET | /api/v1/webhooks | Список webhooks | Менеджер |
| POST | /api/v1/webhooks | Створення webhook | Менеджер |
| DELETE | /api/v1/webhooks/{id} | Видалення webhook | Менеджер |
| GET | /api/v1/analytics/velocity | Швидкість команди по тижнях | Менеджер |
| GET | /api/v1/analytics/cycle-time | Cycle time по виконавцях | Менеджер |
| GET | /api/v1/analytics/cumulative-flow | Cumulative flow diagram | Менеджер |
| GET | /api/v1/analytics/heatmap | Heatmap активності | Менеджер |
| GET | /api/v1/analytics/burndown | Burndown chart | Менеджер |
| GET | /api/v1/tasks/export/csv | Експорт задач у CSV | Менеджер |
| POST | /api/v1/tasks/{id}/voice | Завантаження голосової нотатки | Всі |

---

## 10. НОВІ ТАБЛИЦІ БД

### task_templates
| Колонка | Тип | Опис |
|---|---|---|
| id | TEXT (UUID) PK | Ідентифікатор |
| team_id | TEXT FK → teams.id | Команда |
| name | TEXT NOT NULL | Назва шаблону |
| title_template | TEXT NOT NULL | Шаблон заголовка |
| description_template | TEXT | Шаблон опису |
| priority | TEXT DEFAULT 'medium' | Пріоритет за замовчуванням |
| tags | TEXT DEFAULT '[]' | Теги за замовчуванням |
| estimated_hours | REAL | Оцінка часу |
| cron_schedule | TEXT NULLABLE | CRON-вираз для повторення (NULL = не повторюється) |
| is_active | INTEGER DEFAULT 1 | Активний |
| created_at | TEXT | Дата створення |

### labels
| Колонка | Тип | Опис |
|---|---|---|
| id | TEXT (UUID) PK | Ідентифікатор |
| team_id | TEXT FK → teams.id | Команда |
| name | TEXT NOT NULL | Назва мітки |
| color | TEXT NOT NULL | HEX-колір (#FF5722) |
| created_at | TEXT | Дата створення |

### task_labels (зв'язок many-to-many)
| Колонка | Тип | Опис |
|---|---|---|
| task_id | TEXT FK → tasks.id | Задача |
| label_id | TEXT FK → labels.id | Мітка |
| PK: (task_id, label_id) | | |

### webhooks
| Колонка | Тип | Опис |
|---|---|---|
| id | TEXT (UUID) PK | Ідентифікатор |
| team_id | TEXT FK → teams.id | Команда |
| url | TEXT NOT NULL | URL для POST-запитів |
| events | TEXT NOT NULL | JSON-масив подій: ["task.created","task.completed",...] |
| secret | TEXT | Секрет для HMAC-підпису payload |
| is_active | INTEGER DEFAULT 1 | Активний |
| created_at | TEXT | Дата створення |

### webhook_deliveries
| Колонка | Тип | Опис |
|---|---|---|
| id | TEXT (UUID) PK | Ідентифікатор |
| webhook_id | TEXT FK → webhooks.id | Webhook |
| event | TEXT NOT NULL | Тип події |
| payload | TEXT NOT NULL | JSON payload що було відправлено |
| response_status | INTEGER | HTTP-код відповіді |
| response_body | TEXT | Тіло відповіді (обрізане до 1000 символів) |
| success | INTEGER | 1 = успішно, 0 = помилка |
| created_at | TEXT | Час доставки |

### audit_log
| Колонка | Тип | Опис |
|---|---|---|
| id | TEXT (UUID) PK | Ідентифікатор |
| user_id | TEXT FK → users.id | Хто виконав дію |
| action | TEXT NOT NULL | Тип дії (user.login, task.create, task.delete, ...) |
| entity_type | TEXT | Тип сутності (task, user, board, ...) |
| entity_id | TEXT | ID сутності |
| details | TEXT | JSON з деталями дії |
| ip_address | TEXT | IP-адреса клієнта |
| user_agent | TEXT | User-Agent клієнта |
| created_at | TEXT | Час дії |
