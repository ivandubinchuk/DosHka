"""
Сідер для заповнення БД тестовими даними
"""
import asyncio
import random
from datetime import datetime, timedelta
from uuid import uuid4
import bcrypt
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker

from app.core.config import settings
from app.models import User, Team, Board, Column, Task, Message


def hash_password(password: str) -> str:
    """Hash password using bcrypt"""
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

# Тестові дані
TEAM_NAME = "Dream Team"

MANAGERS = [
    {"email": "manager@test.com", "password": "123456", "full_name": "Олексій Керівник"},
    {"email": "manager2@test.com", "password": "123456", "full_name": "Марія Директор"},
]

EXECUTORS = [
    {"email": "dev1@test.com", "password": "123456", "full_name": "Іван Розробник"},
    {"email": "dev2@test.com", "password": "123456", "full_name": "Петро Кодер"},
    {"email": "dev3@test.com", "password": "123456", "full_name": "Анна Програміст"},
    {"email": "dev4@test.com", "password": "123456", "full_name": "Олена Девелопер"},
    {"email": "dev5@test.com", "password": "123456", "full_name": "Микола Бекендер"},
    {"email": "dev6@test.com", "password": "123456", "full_name": "Софія Фронтендер"},
    {"email": "dev7@test.com", "password": "123456", "full_name": "Дмитро Фуллстек"},
    {"email": "dev8@test.com", "password": "123456", "full_name": "Юлія Тестер"},
    {"email": "dev9@test.com", "password": "123456", "full_name": "Артем Дизайнер"},
    {"email": "dev10@test.com", "password": "123456", "full_name": "Катерина Аналітик"},
]

TASK_TITLES = [
    "Виправити баг в авторизації",
    "Додати темну тему",
    "Оптимізувати запити до БД",
    "Написати unit-тести",
    "Рефакторинг модуля оплат",
    "Інтеграція з Telegram",
    "Налаштувати CI/CD",
    "Оновити документацію API",
    "Редизайн головної сторінки",
    "Додати експорт в PDF",
    "Виправити responsive дизайн",
    "Оптимізація завантаження зображень",
    "Додати push-сповіщення",
    "Міграція на нову версію фреймворку",
    "Інтеграція платіжної системи",
    "Аудит безпеки",
    "Покращити UX форми реєстрації",
    "Додати фільтрацію задач",
    "Реалізувати пошук",
    "Кешування API відповідей",
    "Логування помилок",
    "Моніторинг продуктивності",
    "Налаштування бекапів",
    "Оновити залежності",
    "Виправити витік пам'яті",
    "Додати підтримку мультимови",
    "Інтеграція з Google Calendar",
    "Автоматичне тестування",
    "Code review процес",
    "Документація для нових розробників",
    "Налаштування staging середовища",
    "Оптимізація SEO",
    "Додати аналітику",
    "A/B тестування",
    "Виправити баг в чаті",
    "Додати emoji підтримку",
    "Покращити швидкість завантаження",
    "Рефакторинг CSS",
    "Міграція БД",
    "Додати веб-сокети",
    "Реалізувати drag-n-drop",
    "Покращити валідацію форм",
    "Додати капчу",
    "Інтеграція OAuth",
    "Налаштування CDN",
    "Оптимізація бандлу",
    "Додати lazy loading",
    "Виправити баг в нотифікаціях",
    "Покращити обробку помилок",
    "Додати підказки користувачу",
]

MESSAGES = [
    "Почав працювати над цим",
    "Потрібна допомога з API",
    "Готово на 50%",
    "Знайшов проблему, виправляю",
    "Можеш глянути мій PR?",
    "Тести пройшли успішно",
    "Потрібен code review",
    "Є питання по вимогах",
    "Зробив, перевір будь ласка",
    "Натрапив на баг, досліджую",
    "Оновив документацію",
    "Потрібно обговорити архітектуру",
    "Все готово до деплою",
    "Відкатив зміни, були проблеми",
    "Переніс на завтра, не встигаю",
    "Дякую за фідбек!",
    "Виправив, подивись",
    "Потрібні додаткові дані",
    "Зробив рефакторинг",
    "Тестую на staging",
    "Все ок, мержу",
    "Є конфлікти, розрулюю",
    "Додав коментарі в код",
    "Оптимізував запит, тепер швидше",
    "Потрібна консультація",
]

async def seed_database():
    """Заповнює БД тестовими даними"""

    engine = create_async_engine(settings.DATABASE_URL, echo=False)
    async_session = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    async with async_session() as session:
        print("[*] Pochynayemo zapovnennya bazy danykh...")

        # 1. Створюємо першого менеджера (без team_id)
        first_manager = User(
            id=str(uuid4()),
            email=MANAGERS[0]["email"],
            password_hash=hash_password(MANAGERS[0]["password"]),
            full_name=MANAGERS[0]["full_name"],
            role="manager",
            team_id=None,
            is_active=True
        )
        session.add(first_manager)
        await session.flush()
        print(f"[OK] Stvoreno pershogo menedzhera: {first_manager.email}")

        # 2. Створюємо команду з manager_id
        team = Team(
            id=str(uuid4()),
            name=TEAM_NAME,
            description="Naykrashcha komanda rozrobnykiv",
            manager_id=first_manager.id
        )
        session.add(team)
        await session.flush()
        print(f"[OK] Komanda '{TEAM_NAME}' stvorena")

        # 3. Оновлюємо першого менеджера з team_id
        first_manager.team_id = team.id
        await session.flush()

        # 4. Створюємо решту менеджерів
        all_users = [first_manager]
        for m in MANAGERS[1:]:
            user = User(
                id=str(uuid4()),
                email=m["email"],
                password_hash=hash_password(m["password"]),
                full_name=m["full_name"],
                role="manager",
                team_id=team.id,
                is_active=True
            )
            session.add(user)
            all_users.append(user)
        await session.flush()
        print(f"[OK] Stvoreno {len(MANAGERS)} menedzheriv")

        # 3. Створюємо виконавців
        executors = []
        for e in EXECUTORS:
            user = User(
                id=str(uuid4()),
                email=e["email"],
                password_hash=hash_password(e["password"]),
                full_name=e["full_name"],
                role="executor",
                team_id=team.id,
                is_active=True
            )
            session.add(user)
            all_users.append(user)
            executors.append(user)
        await session.flush()
        print(f"[OK] Stvoreno {len(EXECUTORS)} vykonavtsiv")

        # 4. Створюємо дошку
        board = Board(
            id=str(uuid4()),
            name="Main Board",
            description="Main Kanban board",
            team_id=team.id
        )
        session.add(board)
        await session.flush()
        print("[OK] Board created")

        # 5. Створюємо колонки
        columns_data = [
            ("Backlog", 0, None),
            ("To Do", 1, None),
            ("In Progress", 2, None),
            ("Done", 3, None),
        ]
        columns = []
        for name, position, wip_limit in columns_data:
            col = Column(
                id=str(uuid4()),
                name=name,
                board_id=board.id,
                position=position,
                wip_limit=wip_limit
            )
            session.add(col)
            columns.append(col)
        await session.flush()
        print("[OK] Columns created")

        # 6. Створюємо задачі
        priorities = ["low", "medium", "high", "critical"]
        tasks = []

        for i, title in enumerate(TASK_TITLES):
            # Розподіляємо задачі по колонках з урахуванням WIP лімітів
            # Backlog: без ліміту, To Do: 10, In Progress: 5, Done: без ліміту
            if i < 20:
                column = columns[0]  # Backlog (20 задач)
            elif i < 28:
                column = columns[1]  # To Do (8 задач, ліміт 10)
            elif i < 32:
                column = columns[2]  # In Progress (4 задачі, ліміт 5)
            else:
                column = columns[3]  # Done (решта)

            # Випадковий виконавець (може бути None для Backlog)
            assignee = None
            if column.name != "Backlog" and random.random() > 0.2:
                assignee = random.choice(executors)

            # Випадковий дедлайн
            deadline = None
            if random.random() > 0.3:
                days_offset = random.randint(-5, 14)
                deadline = datetime.utcnow() + timedelta(days=days_offset)

            # Оцінка годин
            estimated = random.choice([None, 2, 4, 8, 16, 24, 40])
            actual = None
            if column.name == "Done" and estimated:
                actual = estimated + random.randint(-4, 8)
                if actual < 1:
                    actual = 1

            completed_at = None
            if column.name == "Done":
                completed_at = datetime.utcnow() - timedelta(days=random.randint(0, 14))

            task = Task(
                id=str(uuid4()),
                title=title,
                description=f"Task description: {title}. Execute with quality.",
                column_id=column.id,
                position=i % 10,
                priority=random.choice(priorities),
                assignee_id=assignee.id if assignee else None,
                creator_id=manager.id,  # Всі задачі від головного менеджера
                deadline=deadline,
                estimated_hours=estimated,
                actual_hours=actual,
                completed_at=completed_at,
                created_at=datetime.utcnow() - timedelta(days=random.randint(1, 30))
            )
            session.add(task)
            tasks.append(task)

        await session.flush()
        print(f"[OK] Created {len(tasks)} tasks")

        # 7. Створюємо повідомлення в чатах
        messages_count = 0
        for task in tasks:
            # Більше повідомлень для задач In Progress і Done
            if task.column_id == columns[2].id:  # In Progress
                num_messages = random.randint(3, 8)
            elif task.column_id == columns[3].id:  # Done
                num_messages = random.randint(5, 15)
            elif task.column_id == columns[1].id:  # To Do
                num_messages = random.randint(1, 4)
            else:  # Backlog
                num_messages = random.randint(0, 2)

            for j in range(num_messages):
                sender = random.choice(all_users)
                msg = Message(
                    id=str(uuid4()),
                    task_id=task.id,
                    sender_id=sender.id,
                    content=random.choice(MESSAGES),
                    type="text",
                    created_at=datetime.utcnow() - timedelta(
                        days=random.randint(0, 14),
                        hours=random.randint(0, 23),
                        minutes=random.randint(0, 59)
                    )
                )
                session.add(msg)
                messages_count += 1

        await session.commit()
        print(f"[OK] Created {messages_count} messages")

        print("\n" + "="*50)
        print("[SUCCESS] Database seeded successfully!")
        print("="*50)
        print("\n[INFO] Test accounts:")
        print("-"*50)
        print("[M] MANAGERS:")
        for m in MANAGERS:
            print(f"   Email: {m['email']}")
            print(f"   Password: {m['password']}")
            print()
        print("[E] EXECUTORS (first 2):")
        for e in EXECUTORS[:2]:
            print(f"   Email: {e['email']}")
            print(f"   Password: {e['password']}")
            print()
        print("="*50)


if __name__ == "__main__":
    asyncio.run(seed_database())
