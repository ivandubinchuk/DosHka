"""
Схеми для команд
"""
from datetime import datetime
from pydantic import BaseModel, Field
from .user import UserResponse, UserBrief


class TeamBase(BaseModel):
    """Базова схема команди"""
    name: str = Field(min_length=1, max_length=255)
    description: str | None = None


class TeamCreate(TeamBase):
    """Схема створення команди"""
    pass


class TeamUpdate(BaseModel):
    """Схема оновлення команди"""
    name: str | None = None
    description: str | None = None


class TeamResponse(TeamBase):
    """Відповідь з даними команди"""
    id: str
    manager_id: str
    invite_code: str | None = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class TeamWithMembers(TeamResponse):
    """Команда з учасниками"""
    members: list[UserBrief] = []


class InviteQRResponse(BaseModel):
    """Відповідь з QR-кодом запрошення"""
    qr_code: str  # Base64 зображення
    invite_token: str
    expires_at: datetime
