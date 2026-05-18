"""
Схеми для повідомлень та вкладень
"""
from datetime import datetime
from pydantic import BaseModel, Field
from .user import UserBrief


class MessageBase(BaseModel):
    """Базова схема повідомлення"""
    content: str = Field(min_length=1)
    type: str = Field(default="text", pattern="^(text|voice|system)$")


class MessageCreate(MessageBase):
    """Схема створення повідомлення"""
    task_id: str


class MessageResponse(MessageBase):
    """Відповідь з даними повідомлення"""
    id: str
    task_id: str
    sender_id: str | None = None
    sender: UserBrief | None = None
    voice_url: str | None = None
    voice_duration: int | None = None
    is_read: bool
    created_at: datetime

    class Config:
        from_attributes = True


class AttachmentBase(BaseModel):
    """Базова схема вкладення"""
    file_name: str
    file_type: str
    file_size: int


class AttachmentResponse(AttachmentBase):
    """Відповідь з даними вкладення"""
    id: str
    task_id: str
    uploader_id: str | None = None
    uploader: UserBrief | None = None
    url: str
    thumbnail_url: str | None = None
    created_at: datetime

    class Config:
        from_attributes = True
