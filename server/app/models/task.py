"""
Модель задачі
"""
import uuid
from datetime import datetime
import sqlalchemy
from sqlalchemy import String, Boolean, DateTime, Integer, Float, Text, ForeignKey, Index, Table
from sqlalchemy.orm import Mapped, mapped_column, relationship
from ..core.database import Base


# Зв'язок many-to-many між задачами та мітками
task_labels = Table(
    "task_labels",
    Base.metadata,
    sqlalchemy.Column("task_id", String(36), ForeignKey("tasks.id", ondelete="CASCADE"), primary_key=True),
    sqlalchemy.Column("label_id", String(36), ForeignKey("labels.id", ondelete="CASCADE"), primary_key=True)
)


class Task(Base):
    """Таблиця задач"""
    __tablename__ = "tasks"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    title: Mapped[str] = mapped_column(String(500), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    column_id: Mapped[str] = mapped_column(String(36), ForeignKey("columns.id", ondelete="CASCADE"), nullable=False)
    position: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    priority: Mapped[str] = mapped_column(String(20), nullable=False, default="medium")  # critical, high, medium, low
    assignee_id: Mapped[str | None] = mapped_column(String(36), ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    creator_id: Mapped[str | None] = mapped_column(String(36), ForeignKey("users.id", ondelete="SET NULL"), nullable=True)
    deadline: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    estimated_hours: Mapped[float | None] = mapped_column(Float, nullable=True)
    actual_hours: Mapped[float | None] = mapped_column(Float, nullable=True)
    tags: Mapped[str] = mapped_column(Text, default="[]")  # JSON масив
    is_deleted: Mapped[bool] = mapped_column(Boolean, default=False)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    version: Mapped[int] = mapped_column(Integer, default=0)  # Optimistic locking

    # Відносини
    column = relationship("Column", back_populates="tasks")
    assignee = relationship("User", back_populates="assigned_tasks", foreign_keys=[assignee_id])
    creator = relationship("User", back_populates="created_tasks", foreign_keys=[creator_id])
    labels = relationship("Label", secondary=task_labels, back_populates="tasks")
    messages = relationship("Message", back_populates="task", cascade="all, delete-orphan")
    attachments = relationship("Attachment", back_populates="task", cascade="all, delete-orphan")

    __table_args__ = (
        Index("ix_tasks_column_id", "column_id"),
        Index("ix_tasks_assignee_id", "assignee_id"),
        Index("ix_tasks_deadline", "deadline"),
        Index("ix_tasks_priority", "priority"),
    )


class Label(Base):
    """Таблиця міток"""
    __tablename__ = "labels"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    color: Mapped[str] = mapped_column(String(20), nullable=False)  # HEX колір
    team_id: Mapped[str] = mapped_column(String(36), ForeignKey("teams.id", ondelete="CASCADE"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    # Відносини
    team = relationship("Team", back_populates="labels")
    tasks = relationship("Task", secondary=task_labels, back_populates="labels")

    __table_args__ = (
        Index("ix_labels_team_id", "team_id"),
    )
