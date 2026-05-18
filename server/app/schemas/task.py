"""
Схеми для задач та міток
"""
from datetime import datetime
from pydantic import BaseModel, Field
from .user import UserBrief


class LabelBase(BaseModel):
    """Базова схема мітки"""
    name: str = Field(min_length=1, max_length=100)
    color: str = Field(pattern="^#[0-9A-Fa-f]{6}$")  # HEX колір


class LabelCreate(LabelBase):
    """Схема створення мітки"""
    team_id: str


class LabelResponse(LabelBase):
    """Відповідь з даними мітки"""
    id: str
    team_id: str
    created_at: datetime

    class Config:
        from_attributes = True


class TaskBase(BaseModel):
    """Базова схема задачі"""
    title: str = Field(min_length=1, max_length=500)
    description: str | None = None
    priority: str = Field(default="medium", pattern="^(critical|high|medium|low)$")
    deadline: datetime | None = None
    estimated_hours: float | None = None
    tags: list[str] = []


class TaskCreate(TaskBase):
    """Схема створення задачі"""
    column_id: str
    assignee_id: str | None = None
    label_ids: list[str] = []


class TaskUpdate(BaseModel):
    """Схема оновлення задачі"""
    title: str | None = None
    description: str | None = None
    priority: str | None = None
    assignee_id: str | None = None
    deadline: datetime | None = None
    estimated_hours: float | None = None
    actual_hours: float | None = None
    tags: list[str] | None = None
    label_ids: list[str] | None = None
    version: int | None = None  # Для optimistic locking


class MoveTaskRequest(BaseModel):
    """Запит на переміщення задачі"""
    column_id: str
    position: int


class TaskResponse(TaskBase):
    """Відповідь з даними задачі"""
    id: str
    column_id: str
    position: int
    assignee_id: str | None = None
    assignee: UserBrief | None = None
    creator_id: str | None = None
    creator: UserBrief | None = None
    actual_hours: float | None = None
    labels: list[LabelResponse] = []
    attachments_count: int = 0
    comments_count: int = 0
    is_deleted: bool = False
    completed_at: datetime | None = None
    created_at: datetime
    updated_at: datetime
    version: int = 0  # Optimistic locking version

    class Config:
        from_attributes = True


class TaskBrief(BaseModel):
    """Коротка інформація про задачу"""
    id: str
    title: str
    priority: str
    deadline: datetime | None = None
    assignee: UserBrief | None = None

    class Config:
        from_attributes = True
