"""
Конфігурація сервера DoshKa
"""
from pydantic_settings import BaseSettings
from pydantic import field_validator
from functools import lru_cache
import json


class Settings(BaseSettings):
    """Налаштування додатку"""

    # Загальні
    APP_NAME: str = "DoshKa API"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True

    # База даних
    DATABASE_URL: str = "sqlite+aiosqlite:///./data/doshka.db"

    # JWT
    SECRET_KEY: str = "doshka-super-secret-key-change-in-production-2024"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7

    # CORS
    CORS_ORIGINS: list[str] = ["*"]

    @field_validator("CORS_ORIGINS", mode="before")
    @classmethod
    def parse_cors_origins(cls, v):
        """Парсить CORS_ORIGINS з рядка через кому або JSON масиву"""
        if isinstance(v, list):
            return v
        if isinstance(v, str):
            # Спробуємо JSON
            if v.startswith("["):
                try:
                    return json.loads(v)
                except json.JSONDecodeError:
                    pass
            # Інакше — через кому
            return [origin.strip() for origin in v.split(",") if origin.strip()]
        return ["*"]

    # Сервер
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    """Отримати налаштування (з кешуванням)"""
    return Settings()


settings = get_settings()
