"""
DoshKa Server - Головний модуль
Канбан-дошка в кишені
"""
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .core.config import settings
from .core.database import engine, Base
from .api import api_router
from .discovery import discovery_server


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Життєвий цикл застосунку"""
    # Створення таблиць при старті
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    # Запуск UDP discovery сервера
    discovery_server.start()

    yield

    # Зупинка discovery сервера
    discovery_server.stop()

    # Закриття з'єднань при зупинці
    await engine.dispose()


app = FastAPI(
    title="DoshKa API",
    description="Канбан-дошка в кишені. Постав. Контролюй. Закрий.",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS налаштування
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Підключення роутерів
app.include_router(api_router)


@app.get("/", tags=["Корінь"])
async def root():
    """Перевірка працездатності сервера"""
    return {
        "name": "DoshKa API",
        "version": "1.0.0",
        "status": "працює",
        "slogan": "Постав. Контролюй. Закрий."
    }



@app.get("/health", tags=["Здоров'я"])
async def health_check():
    """Перевірка стану сервера"""
    return {"status": "healthy"}


if __name__ == "__main__":
    import uvicorn
    # Запуск на всіх інтерфейсах (0.0.0.0) для доступу з телефону
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
