"""
Налаштування бази даних SQLAlchemy Async
"""
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy import event
from .config import settings


# Створюємо async engine
engine = create_async_engine(
    settings.DATABASE_URL,
    echo=settings.DEBUG,
    future=True
)

# Фабрика сесій
async_session_maker = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False
)


class Base(DeclarativeBase):
    """Базовий клас для всіх моделей"""
    pass


async def get_db() -> AsyncSession:
    """Dependency для отримання сесії БД"""
    async with async_session_maker() as session:
        try:
            yield session
        finally:
            await session.close()


async def init_db():
    """Ініціалізація БД (створення таблиць)"""
    async with engine.begin() as conn:
        # Вмикаємо foreign keys для SQLite
        await conn.execute("PRAGMA foreign_keys=ON")
        await conn.run_sync(Base.metadata.create_all)


async def close_db():
    """Закриття з'єднання з БД"""
    await engine.dispose()
