"""
Схеми для дошок та колонок
"""
from datetime import datetime
from pydantic import BaseModel, Field


class ColumnBase(BaseModel):
    """Базова схема колонки"""
    name: str = Field(min_length=1, max_length=255)
    position: int = 0
    wip_limit: int | None = None
    color: str | None = None


class ColumnCreate(ColumnBase):
    """Схема створення колонки"""
    board_id: str


class ColumnUpdate(BaseModel):
    """Схема оновлення колонки"""
    name: str | None = None
    position: int | None = None
    wip_limit: int | None = None
    color: str | None = None


class ColumnResponse(ColumnBase):
    """Відповідь з даними колонки"""
    id: str
    board_id: str
    task_count: int = 0
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class BoardBase(BaseModel):
    """Базова схема дошки"""
    name: str = Field(min_length=1, max_length=255)
    description: str | None = None


class BoardCreate(BoardBase):
    """Схема створення дошки"""
    team_id: str


class BoardUpdate(BaseModel):
    """Схема оновлення дошки"""
    name: str | None = None
    description: str | None = None


class BoardResponse(BoardBase):
    """Відповідь з даними дошки"""
    id: str
    team_id: str
    is_default: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class BoardWithColumns(BoardResponse):
    """Дошка з колонками"""
    columns: list[ColumnResponse] = []
