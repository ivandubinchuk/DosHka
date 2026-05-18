"""
Схеми для користувачів
"""
from datetime import datetime
from pydantic import BaseModel, EmailStr, Field


class UserBase(BaseModel):
    """Базова схема користувача"""
    email: EmailStr
    full_name: str
    role: str
    avatar_url: str | None = None


class UserCreate(UserBase):
    """Схема створення користувача"""
    password: str = Field(min_length=6)


class UserUpdate(BaseModel):
    """Схема оновлення профілю"""
    full_name: str | None = None
    avatar_url: str | None = None


class UserResponse(UserBase):
    """Відповідь з даними користувача"""
    id: str
    team_id: str | None = None
    is_active: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class UserBrief(BaseModel):
    """Коротка інформація про користувача"""
    id: str
    full_name: str
    avatar_url: str | None = None

    class Config:
        from_attributes = True
